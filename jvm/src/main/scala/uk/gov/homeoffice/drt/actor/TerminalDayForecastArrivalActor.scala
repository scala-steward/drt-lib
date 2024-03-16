package uk.gov.homeoffice.drt.actor

import akka.persistence.SaveSnapshotSuccess
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.commands.Commands.GetState
import uk.gov.homeoffice.drt.actor.commands.MergeArrivalsRequest
import uk.gov.homeoffice.drt.arrivals._
import uk.gov.homeoffice.drt.ports.FeedSource
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.protobuf.messages.CrunchState.{FlightsWithSplitsDiffMessage, FlightsWithSplitsMessage, SplitsForArrivalsMessage}
import uk.gov.homeoffice.drt.protobuf.messages.FeedArrivalsMessage.{ForecastArrivalStateSnapshotMessage, ForecastFeedArrivalMessage, ForecastFeedArrivalsDiffMessage}
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage.FlightsDiffMessage
import uk.gov.homeoffice.drt.protobuf.serialisation.{FeedArrivalMessageConversion, FlightMessageConversion}
import uk.gov.homeoffice.drt.protobuf.serialisation.FlightMessageConversion._
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

case class FeedArrivalsDiff[A <: FeedArrival](updates: Iterable[A], removals: Iterable[UniqueArrival]) {
  lazy val isEmpty: Boolean = updates.isEmpty && removals.isEmpty
  lazy val nonEmpty: Boolean = !isEmpty
}

class TerminalDayForecastArrivalActor(year: Int,
                                      month: Int,
                                      day: Int,
                                      terminal: Terminal,
                                      now: () => SDateLike,
                                      maybePointInTime: Option[Long],
                                      feedSource: FeedSource,
                                     ) extends PartitionActor[Map[UniqueArrival, ForecastArrival], FeedArrivalsDiff[ForecastArrival]] {
  override val emptyState: Map[UniqueArrival, ForecastArrival] = Map.empty

  override val eventToMaybeMessage: PartialFunction[(FeedArrivalsDiff[ForecastArrival], Map[UniqueArrival, ForecastArrival]), Option[GeneratedMessage]] = {
    case (diff, state) =>
      val validatedDiff = diff.copy(
        updates = diff.updates.filterNot(u => state.get(u.unique).contains(u)),
        removals = diff.removals.filter(state.contains),
      )
      val updatesForDiff = diff.updates.foldLeft(Seq.empty[ForecastFeedArrivalMessage]) {
        case (acc, a) =>
          state.get(a.unique) match {
            case Some(existing) if existing != a =>
              acc :+ FeedArrivalMessageConversion.forecastArrivalToMessage(a)
            case _ => acc
          }
      }
      val removalsForDiff = diff.removals.map(uniqueArrivalToMessage).toSeq

      if (validatedDiff.nonEmpty) {
        val msg = ForecastFeedArrivalsDiffMessage(Option(now().millisSinceEpoch), removalsForDiff, updatesForDiff)
        Option(msg)
      } else None
  }

  override val messageToState: (GeneratedMessage, Map[UniqueArrival, ForecastArrival]) => Map[UniqueArrival, ForecastArrival] = {
    case (msg: ForecastFeedArrivalsDiffMessage, state) =>
      val removals = msg.removals.map(FlightMessageConversion.uniqueArrivalFromMessage)
      val updates = msg.forecastUpdates.map(FeedArrivalMessageConversion.forecastArrivalFromMessage)
      (state -- removals) ++ updates.map(a => a.unique -> a)
  }

  override val maybeMessageToMaybeAck: Option[GeneratedMessage] => Option[Any] =
    maybeMsg => Option(maybeMsg.nonEmpty)

  override val stateToSnapshotMessage: Map[UniqueArrival, ForecastArrival] => GeneratedMessage =
    state => FeedArrivalMessageConversion.forecastArrivalsToSnapshot(state.values.toSeq)

  override val stateFromSnapshotMessage: GeneratedMessage => Map[UniqueArrival, ForecastArrival] = {
    case ForecastArrivalStateSnapshotMessage(arrivalMessages) =>
      arrivalMessages
        .map(FeedArrivalMessageConversion.forecastArrivalFromMessage)
        .map(a => a.unique -> a)
        .toMap
  }

  val firstMinuteOfDay: SDateLike = SDate(year, month, day, 0, 0)
  private val lastMinuteOfDay: SDateLike = firstMinuteOfDay.addDays(1).addMinutes(-1)

  override val log: Logger = LoggerFactory.getLogger(f"$getClass-$terminal-$year%04d-$month%02d-$day%02d$loggerSuffix")

  override def persistenceId: String = f"${feedSource.id}-feed-arrivals-${terminal.toString.toLowerCase}-$year-$month%02d-$day%02d"

  private val maxSnapshotInterval = 250
  override val maybeSnapshotInterval: Option[Int] = Option(maxSnapshotInterval)

  override def receiveCommand: Receive = {
    case diff: FeedArrivalsDiff[ForecastArrival] =>
      val validatedDiff = diff.copy(
        updates = diff.updates.filterNot(u => state.get(u.unique).contains(u)),
        removals = diff.removals.filter(state.contains),
      )

      updateAndPersistForecastDiffAndAck(validatedDiff)

    case GetState =>
      sender() ! state

    case _: SaveSnapshotSuccess =>
      ackIfRequired()

    case m => log.error(s"Got unexpected message: $m")
  }

  private def updateAndPersistForecastDiffAndAck(diff: FeedArrivalsDiff[ForecastArrival]): Unit = {
    if (!diff.isEmpty) {
      val updatesForDiff = diff.updates.foldLeft(Seq.empty[ForecastFeedArrivalMessage]) {
        case (acc, a) =>
          state.get(a.unique) match {
            case Some(existing) if existing != a =>
              acc :+ FeedArrivalMessageConversion.forecastArrivalToMessage(a)
            case _ => acc
          }
      }
      val removalsForDiff = diff.removals.map(uniqueArrivalToMessage).toSeq

      val msg = ForecastFeedArrivalsDiffMessage(Option(now().millisSinceEpoch), removalsForDiff, updatesForDiff)
      val updateRequests = diff.updates.map(a => MergeArrivalsRequest(SDate(a.scheduled).toUtcDate)).toSet
      val removalRequests = diff.removals.map(ua => MergeArrivalsRequest(SDate(ua.scheduled).toUtcDate)).toSet

      persistAndMaybeSnapshotWithAck(msg, List((sender(), updateRequests ++ removalRequests)))
    }
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
}

