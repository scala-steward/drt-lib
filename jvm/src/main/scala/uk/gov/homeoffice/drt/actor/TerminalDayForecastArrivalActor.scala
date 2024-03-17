package uk.gov.homeoffice.drt.actor

import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.TerminalDayForecastArrivalActor.{Command, GetState}
import uk.gov.homeoffice.drt.arrivals._
import uk.gov.homeoffice.drt.ports.FeedSource
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.protobuf.messages.FeedArrivalsMessage.{ForecastArrivalStateSnapshotMessage, ForecastFeedArrivalMessage, ForecastFeedArrivalsDiffMessage, LiveFeedArrivalMessage, LiveFeedArrivalsDiffMessage}
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage.UniqueArrivalMessage
import uk.gov.homeoffice.drt.protobuf.serialisation.FlightMessageConversion._
import uk.gov.homeoffice.drt.protobuf.serialisation.{FeedArrivalMessageConversion, FlightMessageConversion}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

case class FeedArrivalsDiff[A <: FeedArrival](updates: Iterable[A], removals: Iterable[UniqueArrival]) {
  lazy val isEmpty: Boolean = updates.isEmpty && removals.isEmpty
  lazy val nonEmpty: Boolean = !isEmpty
}

object TerminalDayForecastArrivalActor {
  trait Command

  object GetState extends Command

  val forecastDiffToMaybeMessage =
    diffToMaybeMessage(
      () => SDate.now(),
      arrivalsToMessages(FeedArrivalMessageConversion.forecastArrivalToMessage),
      (n, u, msgs) => ForecastFeedArrivalsDiffMessage(Option(n), msgs, u)
    )

  def diffToMaybeMessage[A <: FeedArrival, U, R]
  (now: () => SDateLike,
   arrivalsToMessages: (FeedArrivalsDiff[A], Map[UniqueArrival, A]) => U,
   toMessage: (Long, U, Seq[UniqueArrivalMessage]) => GeneratedMessage
  ):
  PartialFunction[(FeedArrivalsDiff[A], Map[UniqueArrival, A]), Option[GeneratedMessage]] = {
    case (diff, state) =>
      val validatedDiff = validateDiff(diff, state)
      val updatesForDiff = arrivalsToMessages(diff, state)
      val removalsForDiff: Seq[UniqueArrivalMessage] = diff.removals.map(uniqueArrivalToMessage).toSeq

      if (validatedDiff.nonEmpty) {
        val msg = toMessage(now().millisSinceEpoch, updatesForDiff, removalsForDiff)
        Option(msg)
      } else None
  }

  private def toMessage(now: () => SDateLike, updatesForDiff: Seq[ForecastFeedArrivalMessage], removalsForDiff: Seq[UniqueArrivalMessage]): ForecastFeedArrivalsDiffMessage = {
    ForecastFeedArrivalsDiffMessage(Option(now().millisSinceEpoch), removalsForDiff, updatesForDiff)
  }

  private def arrivalsToMessages[A <: FeedArrival, M](arrivalToMessage: A => M)
                                                     (diff: FeedArrivalsDiff[A],
                                                      state: Map[UniqueArrival, A],
                                                     ): Seq[M] =
    diff.updates.foldLeft(Seq.empty[M]) {
      case (acc, a) =>
        state.get(a.unique) match {
          case Some(existing) if existing != a =>
            acc :+ arrivalToMessage
          case _ => acc
        }
    }

  def liveDiffToMaybeMessage(now: () => SDateLike): PartialFunction[(FeedArrivalsDiff[LiveArrival], Map[UniqueArrival, LiveArrival]), Option[GeneratedMessage]] = {
    case (diff, state) =>
      val validatedDiff = validateDiff(diff, state)
      val updatesForDiff = arrivalsToMessages(diff, state, FeedArrivalMessageConversion.liveArrivalToMessage)
      val removalsForDiff = diff.removals.map(uniqueArrivalToMessage).toSeq

      if (validatedDiff.nonEmpty) {
        val msg = LiveFeedArrivalsDiffMessage(Option(now().millisSinceEpoch), removalsForDiff, updatesForDiff)
        Option(msg)
      } else None
  }

  private def validateDiff[A <: FeedArrival](diff: FeedArrivalsDiff[A], state: Map[UniqueArrival, A]): FeedArrivalsDiff[LiveArrival] = {
    diff.copy(
      updates = diff.updates.filterNot(u => state.get(u.unique).contains(u)),
      removals = diff.removals.filter(state.contains),
    )
  }
}

class TerminalDayForecastArrivalActor(year: Int,
                                      month: Int,
                                      day: Int,
                                      terminal: Terminal,
                                      now: () => SDateLike,
                                      override val maybePointInTime: Option[Long],
                                      feedSource: FeedSource,
                                     )
  extends PartitionActor[Map[UniqueArrival, ForecastArrival], FeedArrivalsDiff[ForecastArrival], Command] {
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

  override def persistenceId: String = f"${feedSource.id}-feed-arrivals-${terminal.toString.toLowerCase}-$year-$month%02d-$day%02d"

  private val maxSnapshotInterval = 250
  override val maybeSnapshotInterval: Option[Int] = Option(maxSnapshotInterval)
  override val processQuery: Command => Any = {
    case GetState => state
  }
}

