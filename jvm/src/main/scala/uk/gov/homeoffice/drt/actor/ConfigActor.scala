package uk.gov.homeoffice.drt.actor

import akka.actor.ActorRef
import akka.persistence._
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.ConfigActor._
import uk.gov.homeoffice.drt.actor.acking.AckingReceiver.StreamCompleted
import uk.gov.homeoffice.drt.actor.commands.Commands.{AddUpdatesSubscriber, GetState}
import uk.gov.homeoffice.drt.actor.commands.CrunchRequest
import uk.gov.homeoffice.drt.actor.serialisation.{ConfigDeserialiser, ConfigSerialiser, EmptyConfig}
import uk.gov.homeoffice.drt.ports.config.updates.{ConfigUpdate, UpdatesWithHistory}
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import uk.gov.homeoffice.drt.time.{LocalDate, MilliTimes, SDate, SDateLike}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt


object ConfigActor {
  sealed trait Command

  case class SetUpdate[A](update: ConfigUpdate[A], maybeOriginalEffectiveFrom: Option[Long]) extends Command {
    lazy val firstMinuteAffected: Long = maybeOriginalEffectiveFrom match {
      case None => update.effectiveFrom
      case Some(originalEffectiveFrom) =>
        if (update.effectiveFrom < originalEffectiveFrom)
          update.effectiveFrom
        else originalEffectiveFrom
    }
  }

  case class RemoveUpdate(effectiveFrom: MillisSinceEpoch) extends Command
}

class ConfigActor[B, A <: UpdatesWithHistory[B]](val persistenceId: String,
                                                 val now: () => SDateLike,
                                                 crunchRequest: MillisSinceEpoch => CrunchRequest,
                                                 maxForecastDays: Int,
                                                )
                                                (implicit
                                                 emptyProvider: EmptyConfig[B, A],
                                                 serialiser: ConfigSerialiser[B, A],
                                                 deserialiser: ConfigDeserialiser[B, A],
                                                ) extends RecoveryActorLike with PersistentDrtActor[A] {
  override val log: Logger = LoggerFactory.getLogger(getClass)

  override val maybeSnapshotInterval: Option[Int] = None

  override def processRecoveryMessage: PartialFunction[Any, Unit] = {
    case msg: GeneratedMessage =>
      state = deserialiser.deserialiseCommand(msg) match {
        case set: SetUpdate[B] => stateUpdate(set)
        case remove: RemoveUpdate => state.remove(remove.effectiveFrom).asInstanceOf[A]
      }
  }

  override def processSnapshotMessage: PartialFunction[Any, Unit] = {
    case msg: GeneratedMessage =>
      state = deserialiser.deserialiseState(msg)
  }

  override def stateToMessage: GeneratedMessage =
    serialiser.updatesWithHistory(state)

  var state: A = emptyProvider.empty

  private var maybeCrunchRequestQueueActor: Option[ActorRef] = None

  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = new Timeout(60.seconds)

  override def initialState: A = emptyProvider.empty

  override def receiveCommand: Receive = {
    case AddUpdatesSubscriber(crunchRequestQueue) =>
      log.info("Received crunch request actor")
      maybeCrunchRequestQueueActor = Option(crunchRequestQueue)

    case updates: SetUpdate[B] =>
      state = stateUpdate(updates)
      persistAndMaybeSnapshot(serialiser.setUpdate(updates))
      sendCrunchRequests(SDate(updates.firstMinuteAffected).toLocalDate)

      sender() ! updates

    case GetState =>
      sender() ! state

    case remove: RemoveUpdate =>
      state = state.remove(remove.effectiveFrom).asInstanceOf[A]
      persistAndMaybeSnapshot(serialiser.removeUpdate(remove))
      sendCrunchRequests(SDate(remove.effectiveFrom).toLocalDate)

      sender() ! remove

    case SaveSnapshotSuccess(md) =>
      log.debug(s"Save snapshot success: $md")

    case SaveSnapshotFailure(md, cause) =>
      log.error(s"Save snapshot failure: $md", cause)

    case StreamCompleted => log.warn("Received shutdown")

    case unexpected => log.error(s"Received unexpected message ${unexpected.getClass}")
  }

  private def sendCrunchRequests(firstDay: LocalDate): Unit =
    maybeCrunchRequestQueueActor.foreach { requestActor =>
      (SDate(firstDay).millisSinceEpoch to SDate.now().addDays(maxForecastDays).millisSinceEpoch by MilliTimes.oneHourMillis).map { millis =>
        requestActor ! crunchRequest(millis)
      }
    }

  private def stateUpdate(updates: SetUpdate[B]): A = {
    val updated = updates.maybeOriginalEffectiveFrom match {
      case None =>
        state.update(updates.update.effectiveFrom, updates.update.item)
      case Some(originalEffectiveFrom) =>
        state.update(updates.update.effectiveFrom, updates.update.item, originalEffectiveFrom)
    }
    updated.asInstanceOf[A]
  }
}
