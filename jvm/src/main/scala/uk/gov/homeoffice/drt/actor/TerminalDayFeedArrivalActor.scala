package uk.gov.homeoffice.drt.actor

import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.TerminalDayFeedArrivalActor.{Command, GetState}
import uk.gov.homeoffice.drt.arrivals._
import uk.gov.homeoffice.drt.ports.Terminals.{T1, Terminal}
import uk.gov.homeoffice.drt.ports.{FeedSource, ForecastFeedSource}
import uk.gov.homeoffice.drt.protobuf.messages.FeedArrivalsMessage.{ForecastArrivalStateSnapshotMessage, ForecastFeedArrivalsDiffMessage, LiveArrivalStateSnapshotMessage, LiveFeedArrivalsDiffMessage}
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage.UniqueArrivalMessage
import uk.gov.homeoffice.drt.protobuf.serialisation.FlightMessageConversion._
import uk.gov.homeoffice.drt.protobuf.serialisation.{FeedArrivalMessageConversion, FlightMessageConversion}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

case class FeedArrivalsDiff[A <: FeedArrival](updates: Iterable[A], removals: Iterable[UniqueArrival]) {
  lazy val isEmpty: Boolean = updates.isEmpty && removals.isEmpty
  lazy val nonEmpty: Boolean = !isEmpty
}

object TerminalDayFeedArrivalActor {
  trait Command

  object GetState extends Command

  val forecastDiffToMaybeMessage: PartialFunction[(FeedArrivalsDiff[ForecastArrival], Map[UniqueArrival, ForecastArrival]), Option[GeneratedMessage]] =
    diffToMaybeMessage(
      () => SDate.now(),
      arrivalsToMessages(FeedArrivalMessageConversion.forecastArrivalToMessage),
      (n, u, msgs) => ForecastFeedArrivalsDiffMessage(Option(n), msgs, u)
    )

  val liveDiffToMaybeMessage: PartialFunction[(FeedArrivalsDiff[LiveArrival], Map[UniqueArrival, LiveArrival]), Option[GeneratedMessage]] =
    diffToMaybeMessage(
      () => SDate.now(),
      arrivalsToMessages(FeedArrivalMessageConversion.liveArrivalToMessage),
      (n, u, msgs) => LiveFeedArrivalsDiffMessage(Option(n), msgs, u)
    )

  def forecastStateFromMessage: (GeneratedMessage, Map[UniqueArrival, ForecastArrival]) => Map[UniqueArrival, ForecastArrival] = {
    case (msg: ForecastFeedArrivalsDiffMessage, state) =>
      val removals = msg.removals.map(FlightMessageConversion.uniqueArrivalFromMessage)
      val updates = msg.forecastUpdates.map(FeedArrivalMessageConversion.forecastArrivalFromMessage)
      (state -- removals) ++ updates.map(a => a.unique -> a)
  }

  def liveStateFromMessage: (GeneratedMessage, Map[UniqueArrival, LiveArrival]) => Map[UniqueArrival, LiveArrival] = {
    case (msg: LiveFeedArrivalsDiffMessage, state) =>
      val removals = msg.removals.map(FlightMessageConversion.uniqueArrivalFromMessage)
      val updates = msg.liveUpdates.map(FeedArrivalMessageConversion.liveArrivalFromMessage)
      (state -- removals) ++ updates.map(a => a.unique -> a)
  }

  val forecastStateToSnapshotMessage: Map[UniqueArrival, ForecastArrival] => GeneratedMessage =
    state => FeedArrivalMessageConversion.forecastArrivalsToSnapshot(state.values.toSeq)

  val liveStateToSnapshotMessage: Map[UniqueArrival, LiveArrival] => GeneratedMessage =
    state => FeedArrivalMessageConversion.liveArrivalsToSnapshot(state.values.toSeq)

  val forecastStateFromSnapshotMessage: GeneratedMessage => Map[UniqueArrival, ForecastArrival] = {
    case ForecastArrivalStateSnapshotMessage(arrivalMessages) =>
      arrivalMessages
        .map(FeedArrivalMessageConversion.forecastArrivalFromMessage)
        .map(a => a.unique -> a)
        .toMap
  }

  val liveStateFromSnapshotMessage: GeneratedMessage => Map[UniqueArrival, LiveArrival] = {
    case LiveArrivalStateSnapshotMessage(arrivalMessages) =>
      arrivalMessages
        .map(FeedArrivalMessageConversion.liveArrivalFromMessage)
        .map(a => a.unique -> a)
        .toMap
  }

  private def diffToMaybeMessage[A <: FeedArrival, U, R]
  (now: () => SDateLike,
   arrivalsToMessages: (FeedArrivalsDiff[A], Map[UniqueArrival, A]) => U,
   toMessage: (Long, U, Seq[UniqueArrivalMessage]) => GeneratedMessage
  ): PartialFunction[(FeedArrivalsDiff[A], Map[UniqueArrival, A]), Option[GeneratedMessage]] = {
    case (diff, state) =>
      val validatedDiff = validateDiff(diff, state)
      val updatesForDiff = arrivalsToMessages(diff, state)
      val removalsForDiff: Seq[UniqueArrivalMessage] = diff.removals.map(uniqueArrivalToMessage).toSeq

      if (validatedDiff.nonEmpty) {
        val msg = toMessage(now().millisSinceEpoch, updatesForDiff, removalsForDiff)
        Option(msg)
      } else None
  }

  private def arrivalsToMessages[A <: FeedArrival, M](arrivalToMessage: A => M)
                                                     (diff: FeedArrivalsDiff[A],
                                                      state: Map[UniqueArrival, A],
                                                     ): Seq[M] =
    diff.updates.foldLeft(Seq.empty[M]) {
      case (acc, a) =>
        state.get(a.unique) match {
          case Some(existing) if existing != a =>
            acc :+ arrivalToMessage(a)
          case _ => acc
        }
    }

  private def validateDiff[A <: FeedArrival](diff: FeedArrivalsDiff[A], state: Map[UniqueArrival, A]): FeedArrivalsDiff[A] = {
    diff.copy(
      updates = diff.updates.filterNot(u => state.get(u.unique).contains(u)),
      removals = diff.removals.filter(state.contains),
    )
  }

  def forecast(year: Int,
               month: Int,
               day: Int,
               feedSource: FeedSource,
               maybePointInTime: Option[Long],
              ): TerminalDayFeedArrivalActor[ForecastArrival] = {
    new TerminalDayFeedArrivalActor(year, month, day, T1, feedSource, maybePointInTime,
      eventToMaybeMessage = TerminalDayFeedArrivalActor.forecastDiffToMaybeMessage,
      messageToState = TerminalDayFeedArrivalActor.forecastStateFromMessage,
      stateToSnapshotMessage = TerminalDayFeedArrivalActor.forecastStateToSnapshotMessage,
      stateFromSnapshotMessage = TerminalDayFeedArrivalActor.forecastStateFromSnapshotMessage,
    )
  }

  def live(year: Int,
           month: Int,
           day: Int,
           feedSource: FeedSource,
           maybePointInTime: Option[Long],
          ): TerminalDayFeedArrivalActor[LiveArrival] = {
    new TerminalDayFeedArrivalActor(year, month, day, T1, feedSource, maybePointInTime,
      eventToMaybeMessage = TerminalDayFeedArrivalActor.liveDiffToMaybeMessage,
      messageToState = TerminalDayFeedArrivalActor.liveStateFromMessage,
      stateToSnapshotMessage = TerminalDayFeedArrivalActor.liveStateToSnapshotMessage,
      stateFromSnapshotMessage = TerminalDayFeedArrivalActor.liveStateFromSnapshotMessage,
    )
  }
}

class TerminalDayFeedArrivalActor[A <: FeedArrival](year: Int,
                                                    month: Int,
                                                    day: Int,
                                                    terminal: Terminal,
                                                    feedSource: FeedSource,
                                                    override val maybePointInTime: Option[Long],
                                                    override val eventToMaybeMessage: PartialFunction[(FeedArrivalsDiff[A], Map[UniqueArrival, A]), Option[GeneratedMessage]],
                                                    override val messageToState: (GeneratedMessage, Map[UniqueArrival, A]) => Map[UniqueArrival, A],
                                                    override val stateToSnapshotMessage: Map[UniqueArrival, A] => GeneratedMessage,
                                                    override val stateFromSnapshotMessage: GeneratedMessage => Map[UniqueArrival, A],
                                                   )
  extends PartitionActor[Map[UniqueArrival, A], FeedArrivalsDiff[A], Command] {
  override def persistenceId: String = f"${feedSource.id}-feed-arrivals-${terminal.toString.toLowerCase}-$year-$month%02d-$day%02d"

  override val emptyState: Map[UniqueArrival, A] = Map.empty

  override val maybeMessageToMaybeAck: Option[GeneratedMessage] => Option[Any] =
    maybeMsg => Option(maybeMsg.nonEmpty)

  private val maxSnapshotInterval = 250
  override val maybeSnapshotInterval: Option[Int] = Option(maxSnapshotInterval)
  override val processQuery: Command => Any = {
    case GetState => state
  }
}
