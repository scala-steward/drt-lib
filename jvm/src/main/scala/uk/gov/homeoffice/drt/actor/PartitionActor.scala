package uk.gov.homeoffice.drt.actor

import akka.persistence.SaveSnapshotSuccess
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.time.SDate


trait PartitionActor[S] extends RecoveryActorLike {
  def emptyState: S

  val eventToMaybeMessage: PartialFunction[(Any, S), Option[GeneratedMessage]]
  val messageToState: (GeneratedMessage, S) => S
  val maybeMessageToMaybeAck: Option[GeneratedMessage] => Option[Any]

  val stateToSnapshotMessage: S => GeneratedMessage
  val stateFromSnapshotMessage: GeneratedMessage => S

  val processQuery: PartialFunction[Any, Unit]

  def maybePointInTime: Option[Long]

  private lazy val loggerSuffix: String = maybePointInTime match {
    case None => ""
    case Some(pit) => f"@${SDate(pit).toISOString}"
  }

  protected lazy val log: Logger = LoggerFactory.getLogger(f"$persistenceId$loggerSuffix")

  var state: S = emptyState

  protected val maxSnapshotInterval = 250
  override lazy val maybeSnapshotInterval: Option[Int] = Option(maxSnapshotInterval)

  override def stateToMessage: GeneratedMessage = stateToSnapshotMessage(state)

  override def processSnapshotMessage: PartialFunction[Any, Unit] = {
    case msg: GeneratedMessage =>
      state = stateFromSnapshotMessage(msg)
  }

  override def processRecoveryMessage: PartialFunction[GeneratedMessage, Unit] = {
    case msg =>
      state = messageToState(msg, state)
  }

  private def receiveEvent: Receive = {
    case SaveSnapshotSuccess(metadata) =>
      log.info(s"Snapshot saved: $metadata")
      ackIfRequired()

    case event =>
      val maybeMsg = eventToMaybeMessage(event, state)
      val maybeAck = maybeMessageToMaybeAck(maybeMsg)
      maybeMsg match {
        case Some(msg) =>
          val maybeReplyToWithAck = maybeAck.toList.map(ack => (sender(), ack))
          persistAndMaybeSnapshotWithAck(msg, maybeReplyToWithAck)
          state = messageToState(msg, state)
        case None =>
          maybeAck.foreach(sender() ! _)
      }
  }
  override def receiveCommand: Receive = processQuery orElse receiveEvent
}

