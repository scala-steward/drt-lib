package uk.gov.homeoffice.drt.actor

import akka.persistence.{PersistentActor, SaveSnapshotSuccess}
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.commands.Commands.GetState
import uk.gov.homeoffice.drt.actor.commands.MergeArrivalsRequest
import uk.gov.homeoffice.drt.arrivals._
import uk.gov.homeoffice.drt.protobuf.messages.CrunchState.{FlightsWithSplitsDiffMessage, FlightsWithSplitsMessage, SplitsForArrivalsMessage}
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage.FlightsDiffMessage
import uk.gov.homeoffice.drt.protobuf.serialisation.FlightMessageConversion._
import uk.gov.homeoffice.drt.protobuf.serialisation.{FeedArrivalMessageConversion, FlightMessageConversion}
import uk.gov.homeoffice.drt.time.SDate


trait PartitionActor[A, C] extends PersistentActor {
  def emptyState: A
  def commandToDiffMessage: C => GeneratedMessage
  def messageToState: (GeneratedMessage, A) => A
  def stateToMessage: A => GeneratedMessage
  def stateFromMessage: GeneratedMessage => A
  def maybePointInTime: Option[Long]

  lazy val loggerSuffix: String = maybePointInTime match {
    case None => ""
    case Some(pit) => f"@${SDate(pit).toISOString}"
  }

  private lazy val log: Logger = LoggerFactory.getLogger(f"$persistenceId$loggerSuffix")

  var state: A = emptyState

  private val maxSnapshotInterval = 250
  private val maybeSnapshotInterval: Option[Int] = Option(maxSnapshotInterval)

  override def receiveCommand: Receive = {
    case c: C =>
      val msg = commandToDiffMessage(c)
      state = messageToState(msg, state)

    case GetState =>
      sender() ! state

    case _: SaveSnapshotSuccess =>
      ackIfRequired()

    case m => log.error(s"Got unexpected message: $m")
  }

  override def processRecoveryMessage: PartialFunction[Any, Unit] = {
    case FlightsWithSplitsDiffMessage(Some(createdAt), removals, updates) =>
      maybePointInTime match {
        case Some(pit) if pit < createdAt =>
          log.debug(s"Ignoring diff created more recently than the recovery point in time")
        case _ =>
          if (isBeforeCutoff(createdAt))
            restorer.remove(uniqueArrivalsFromMessages(removals))

          val incomingFws = updates.map(flightWithSplitsFromMessage).map(fws => (fws.unique, fws)).toMap
          val updateFws: (Option[ApiFlightWithSplits], ApiFlightWithSplits) => Option[ApiFlightWithSplits] = (maybeExisting, newFws) =>
            Option(maybeExisting.map(_.update(newFws)).getOrElse(newFws))
          restorer.applyUpdates(incomingFws, updateFws)
      }

    case FlightsDiffMessage(Some(createdAt), removals, updates, _) =>
      maybePointInTime match {
        case Some(pit) if pit < createdAt =>
          log.debug(s"Ignoring diff created more recently than the recovery point in time")
        case _ =>
          if (isBeforeCutoff(createdAt))
            restorer.remove(uniqueArrivalsFromMessages(removals))

          val incomingArrivals = updates.map(flightMessageToApiFlight).map(a => (a.unique, a)).toMap
          val updateFws: (Option[ApiFlightWithSplits], Arrival) => Option[ApiFlightWithSplits] = (maybeExistingFws, incoming) => {
            val updated = maybeExistingFws
              .map(existingFws => existingFws.copy(apiFlight = existingFws.apiFlight.update(incoming)))
              .getOrElse(ApiFlightWithSplits(incoming, Set(), Option(createdAt)))
              .copy(lastUpdated = Option(createdAt))
            Option(updated)
          }

          restorer.applyUpdates(incomingArrivals, updateFws)
      }

    case msg@SplitsForArrivalsMessage(Some(createdAt), _) =>
      maybePointInTime match {
        case Some(pit) if pit < createdAt =>
          log.debug(s"Ignoring diff created more recently than the recovery point in time")
        case _ =>
          val incomingSplits = splitsForArrivalsFromMessage(msg).splits
          val updateFws: (Option[ApiFlightWithSplits], Set[Splits]) => Option[ApiFlightWithSplits] = (maybeFws, incoming) => {
            maybeFws.map(fws => SplitsForArrivals.updateFlightWithSplits(fws, incoming, createdAt))
          }
          restorer.applyUpdates(incomingSplits, updateFws)
      }
  }

  override def processSnapshotMessage: PartialFunction[Any, Unit] = {
    case m: FlightsWithSplitsMessage =>
      val flights = m.flightWithSplits.map(FlightMessageConversion.flightWithSplitsFromMessage)
      restorer.applyUpdates(flights)
  }

  override def stateToMessage: GeneratedMessage = FlightMessageConversion.flightsToMessage(state.flights.values)
}

