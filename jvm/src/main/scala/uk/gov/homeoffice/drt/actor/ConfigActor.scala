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
import uk.gov.homeoffice.drt.ports.config.updates.{ConfigUpdate, Configs}
import uk.gov.homeoffice.drt.protobuf.messages.config.Configs.RemoveConfigMessage
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import uk.gov.homeoffice.drt.time.{LocalDate, MilliTimes, SDate, SDateLike}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object ConfigActor {
  sealed trait Command

  case class SetUpdate[A](update: ConfigUpdate[A]) extends Command {
    lazy val firstMinuteAffected: Long = update.maybeOriginalEffectiveFrom match {
      case None => update.effectiveFrom
      case Some(originalEffectiveFrom) =>
        if (update.effectiveFrom < originalEffectiveFrom)
          update.effectiveFrom
        else originalEffectiveFrom
    }
  }

  case class RemoveConfig(effectiveFrom: MillisSinceEpoch) extends Command
}

class ConfigActor[A, B <: Configs[A]](val persistenceId: String,
                                      val now: () => SDateLike,
                                      crunchRequest: MillisSinceEpoch => CrunchRequest,
                                      maxForecastDays: Int,
                                     )
                                     (implicit
                                      emptyProvider: EmptyConfig[A, B],
                                      serialiser: ConfigSerialiser[A, B],
                                      deserialiser: ConfigDeserialiser[A, B],
                                     ) extends RecoveryActorLike with PersistentDrtActor[B] {
  override val log: Logger = LoggerFactory.getLogger(getClass)

  override val maybeSnapshotInterval: Option[Int] = None

  override def processRecoveryMessage: PartialFunction[Any, Unit] = {
    case msg: RemoveConfigMessage =>
      state = state.remove(deserialiser.removeUpdate(msg).effectiveFrom).asInstanceOf[B]

    case msg: GeneratedMessage =>
      state = deserialiser.deserialiseCommand(msg) match {
        case update: SetUpdate[A] => stateUpdate(update.update)
      }
  }

  override def processSnapshotMessage: PartialFunction[Any, Unit] = {
    case msg: GeneratedMessage =>
      state = deserialiser.deserialiseState(msg)
  }

  override def stateToMessage: GeneratedMessage =
    serialiser.updatesWithHistory(state)

  var state: B = emptyProvider.empty

  private var maybeCrunchRequestQueueActor: Option[ActorRef] = None

  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = new Timeout(60.seconds)

  override def initialState: B = emptyProvider.empty

  override def receiveCommand: Receive = {
    case AddUpdatesSubscriber(crunchRequestQueue) =>
      log.info("Received crunch request actor")
      maybeCrunchRequestQueueActor = Option(crunchRequestQueue)

    case update: SetUpdate[A] =>
      state = stateUpdate(update.update)
      persistAndMaybeSnapshot(serialiser.setUpdate(update))
      sendCrunchRequests(SDate(update.firstMinuteAffected).toLocalDate)

    case GetState =>
      println(s"got state request")
      sender() ! state

    case remove: RemoveConfig =>
      state = state.remove(remove.effectiveFrom).asInstanceOf[B]
      persistAndMaybeSnapshot(serialiser.removeUpdate(remove))
      sendCrunchRequests(SDate(remove.effectiveFrom).toLocalDate)

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

  private def stateUpdate(update: ConfigUpdate[A]): B = state.update(update).asInstanceOf[B]
}
