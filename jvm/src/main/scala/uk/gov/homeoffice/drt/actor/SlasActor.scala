package uk.gov.homeoffice.drt.actor

import akka.actor.ActorRef
import akka.pattern.ask
import akka.persistence._
import akka.stream.QueueOfferResult.Enqueued
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.SlasActor._
import uk.gov.homeoffice.drt.actor.acking.AckingReceiver.StreamCompleted
import uk.gov.homeoffice.drt.actor.commands.Commands.{AddUpdatesSubscriber, GetState}
import uk.gov.homeoffice.drt.actor.commands.CrunchRequest
import uk.gov.homeoffice.drt.actor.serialisation.SlasMessageConversion
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.{SlaUpdates, SlasUpdate}
import uk.gov.homeoffice.drt.protobuf.messages.SlasUpdates.{RemoveSlasUpdateMessage, SetSlasUpdateMessage, SlaUpdatesMessage}
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import uk.gov.homeoffice.drt.time.{MilliTimes, SDate, SDateLike}

import scala.collection.immutable.SortedMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object SlasActor {
  sealed trait Command

  case object SendToSubscriber extends Command

  case object ReceivedSubscriberAck extends Command

  case class SetSlasUpdate(update: SlasUpdate, maybeOriginalEffectiveFrom: Option[Long]) extends Command {
    lazy val firstMinuteAffected: Long = maybeOriginalEffectiveFrom match {
      case None => update.effectiveFrom
      case Some(originalEffectiveFrom) =>
        if (update.effectiveFrom < originalEffectiveFrom)
          update.effectiveFrom
        else originalEffectiveFrom
    }
  }

  //  object SetSlasUpdate {
  //    implicit val rw: ReadWriter[SetSlasUpdate] = macroRW
  //  }

  case class RemoveSlasUpdate(effectiveFrom: MillisSinceEpoch) extends Command

  def slasProvider(egateBanksUpdatesActor: ActorRef)
                  (implicit timeout: Timeout, ec: ExecutionContext): MillisSinceEpoch => Future[Map[Queue, Int]] = (at: MillisSinceEpoch) =>
    egateBanksUpdatesActor
      .ask(GetState)
      .mapTo[SlaUpdates]
      .map(_.updatesForDate(at).getOrElse(throw new Exception(s"No slas found for date $at")))
}

class SlasActor(val now: () => SDateLike,
                crunchRequest: MillisSinceEpoch => CrunchRequest,
                maxForecastDays: Int) extends RecoveryActorLike with PersistentDrtActor[SlaUpdates] {
  override val log: Logger = LoggerFactory.getLogger(getClass)

  override def persistenceId: String = "slas"

  override val maybeSnapshotInterval: Option[Int] = None

  override def processRecoveryMessage: PartialFunction[Any, Unit] = {
    case msg: SetSlasUpdateMessage =>
      state = stateUpdate(SlasMessageConversion.setSlasUpdatesFromMessage(msg))

    case msg: RemoveSlasUpdateMessage =>
      state = state.remove(SlasMessageConversion.removeSlasUpdatesFromMessage(msg).effectiveFrom)
  }

  override def processSnapshotMessage: PartialFunction[Any, Unit] = {
    case smm: SlaUpdatesMessage =>
      state = SlasMessageConversion.slasUpdatesFromMessage(smm)
  }

  override def stateToMessage: GeneratedMessage =
    SlasMessageConversion.slasUpdatesToMessage(state)

  var state: SlaUpdates = SlaUpdates(SortedMap())

  var maybeSubscriber: Option[SourceQueueWithComplete[List[Command]]] = None
  var subscriberMessageQueue: List[Command] = List()
  var awaitingSubscriberAck = false

  var maybeCrunchRequestQueueActor: Option[ActorRef] = None

  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = new Timeout(60.seconds)

  override def initialState: SlaUpdates = SlaUpdates(SortedMap())

  override def receiveCommand: Receive = {
    case AddUpdatesSubscriber(crunchRequestQueue) =>
      log.info("Received crunch request actor")
      maybeCrunchRequestQueueActor = Option(crunchRequestQueue)

    case SendToSubscriber =>
      maybeSubscriber.foreach { sub =>
        log.info("Check if we have something to send")
        if (!awaitingSubscriberAck) {
          if (subscriberMessageQueue.nonEmpty) {
            log.info("Sending egate updates to subscriber")
            sub.offer(subscriberMessageQueue).onComplete {
              case Success(result) =>
                if (result != Enqueued) log.error(s"Failed to enqueue red list updates")
                self ! ReceivedSubscriberAck
              case Failure(t) =>
                log.error(s"Failed to enqueue red list updates", t)
                self ! ReceivedSubscriberAck
            }
            subscriberMessageQueue = List()
            awaitingSubscriberAck = true
          } else log.info("Nothing to send")
        } else log.info("Still awaiting subscriber Ack")
      }

    case ReceivedSubscriberAck =>
      log.info("Received subscriber ack")
      awaitingSubscriberAck = false
      if (subscriberMessageQueue.nonEmpty) self ! SendToSubscriber

    case updates: SetSlasUpdate =>
      log.info(s"Saving updates $updates")

      maybeCrunchRequestQueueActor.foreach { requestActor =>
        (updates.firstMinuteAffected to SDate.now().addDays(maxForecastDays).millisSinceEpoch by MilliTimes.oneHourMillis).map { millis =>
          requestActor ! crunchRequest(millis)
        }
      }

      state = stateUpdate(updates)
      persistAndMaybeSnapshot(SlasMessageConversion.setSlasUpdatesToMessage(updates))
      subscriberMessageQueue = updates :: subscriberMessageQueue
      self ! SendToSubscriber
      sender() ! updates

    case GetState =>
      log.debug(s"Received GetState request. Sending updates with ${state.updates.size} update sets")
      sender() ! state

    case remove: RemoveSlasUpdate =>
      state = state.remove(remove.effectiveFrom)
      persistAndMaybeSnapshot(SlasMessageConversion.removeSlasUpdateToMessage(remove))
      subscriberMessageQueue = remove :: subscriberMessageQueue
      self ! SendToSubscriber
      sender() ! remove

    case SaveSnapshotSuccess(md) =>
      log.info(s"Save snapshot success: $md")

    case SaveSnapshotFailure(md, cause) =>
      log.error(s"Save snapshot failure: $md", cause)

    case StreamCompleted => log.warn("Received shutdown")

    case unexpected => log.error(s"Received unexpected message ${unexpected.getClass}")
  }

  private def stateUpdate(updates: SetSlasUpdate): SlaUpdates =
    updates.maybeOriginalEffectiveFrom match {
      case None =>
        state.update(updates.update.effectiveFrom, updates.update.item)
      case Some(originalEffectiveFrom) =>
        state.update(updates.update.effectiveFrom, updates.update.item, originalEffectiveFrom)
    }

}
