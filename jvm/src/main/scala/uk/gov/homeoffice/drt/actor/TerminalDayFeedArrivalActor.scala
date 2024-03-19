package uk.gov.homeoffice.drt.actor

import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.TerminalDayFeedArrivalActor.{GetState, Query}
import uk.gov.homeoffice.drt.arrivals._
import uk.gov.homeoffice.drt.ports.FeedSource
import uk.gov.homeoffice.drt.ports.Terminals.{T1, Terminal}
import uk.gov.homeoffice.drt.protobuf.messages.FeedArrivalsMessage.{ForecastArrivalStateSnapshotMessage, ForecastFeedArrivalsDiffMessage, LiveArrivalStateSnapshotMessage, LiveFeedArrivalsDiffMessage}
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage.UniqueArrivalMessage
import uk.gov.homeoffice.drt.protobuf.serialisation.FlightMessageConversion._
import uk.gov.homeoffice.drt.protobuf.serialisation.{FeedArrivalMessageConversion, FlightMessageConversion}


object TerminalDayFeedArrivalActor {
  trait Query

  trait Event

  object GetState extends Query

  case class FeedArrivalsDiff[A <: FeedArrival](updates: Iterable[A], removals: Iterable[UniqueArrival]) extends Event {
    lazy val isEmpty: Boolean = updates.isEmpty && removals.isEmpty
    lazy val nonEmpty: Boolean = !isEmpty
  }

  def forecastDiffToMaybeMessage(now: () => Long): PartialFunction[(Any, Map[UniqueArrival, ForecastArrival]), Option[GeneratedMessage]] =
    diffToMaybeMessage(
      now,
      arrivalsToMessages(FeedArrivalMessageConversion.forecastArrivalToMessage),
      (n, u, msgs) => ForecastFeedArrivalsDiffMessage(Option(n), msgs, u)
    )

  def liveDiffToMaybeMessage(now: () => Long): PartialFunction[(Any, Map[UniqueArrival, LiveArrival]), Option[GeneratedMessage]] =
    diffToMaybeMessage(
      now,
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

  private val forecastStateToSnapshotMessage: Map[UniqueArrival, ForecastArrival] => GeneratedMessage =
    state => FeedArrivalMessageConversion.forecastArrivalsToSnapshot(state.values.toSeq)

  private val liveStateToSnapshotMessage: Map[UniqueArrival, LiveArrival] => GeneratedMessage =
    state => FeedArrivalMessageConversion.liveArrivalsToSnapshot(state.values.toSeq)

  private val forecastStateFromSnapshotMessage: GeneratedMessage => Map[UniqueArrival, ForecastArrival] = {
    case ForecastArrivalStateSnapshotMessage(arrivalMessages) =>
      arrivalMessages
        .map(FeedArrivalMessageConversion.forecastArrivalFromMessage)
        .map(a => a.unique -> a)
        .toMap
  }

  private val liveStateFromSnapshotMessage: GeneratedMessage => Map[UniqueArrival, LiveArrival] = {
    case LiveArrivalStateSnapshotMessage(arrivalMessages) =>
      arrivalMessages
        .map(FeedArrivalMessageConversion.liveArrivalFromMessage)
        .map(a => a.unique -> a)
        .toMap
  }

  private def diffToMaybeMessage[A <: FeedArrival, U]
  (now: () => Long,
   arrivalsToMessages: (FeedArrivalsDiff[A], Map[UniqueArrival, A]) => U,
   toMessage: (Long, U, Seq[UniqueArrivalMessage]) => GeneratedMessage
  ): PartialFunction[(Any, Map[UniqueArrival, A]), Option[GeneratedMessage]] = {
    case (diff: FeedArrivalsDiff[A], state) =>
      val validatedDiff = validateDiff(diff, state)
      val updatesForDiff = arrivalsToMessages(diff, state)
      val removalsForDiff: Seq[UniqueArrivalMessage] = diff.removals.map(uniqueArrivalToMessage).toSeq

      if (validatedDiff.nonEmpty) {
        val msg = toMessage(now(), updatesForDiff, removalsForDiff)
        Option(msg)
      } else None

    case unexpected =>
      log.error(s"Unexpected message: $unexpected")
      None
  }

  private def arrivalsToMessages[A <: FeedArrival, M](arrivalToMessage: A => M)
                                                     (diff: FeedArrivalsDiff[A],
                                                      state: Map[UniqueArrival, A],
                                                     ): Seq[M] =
    diff.updates.foldLeft(Seq.empty[M]) {
      case (acc, a) =>
        state.get(a.unique) match {
          case Some(existing) if existing == a =>
            acc
          case _ =>
            acc :+ arrivalToMessage(a)
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
               now: () => Long,
               maxSnapshotInterval: Int = 250,
              ): TerminalDayFeedArrivalActor[ForecastArrival] = {
    new TerminalDayFeedArrivalActor(year, month, day, T1, feedSource, maybePointInTime,
      eventToMaybeMessage = TerminalDayFeedArrivalActor.forecastDiffToMaybeMessage(now),
      messageToState = TerminalDayFeedArrivalActor.forecastStateFromMessage,
      stateToSnapshotMessage = TerminalDayFeedArrivalActor.forecastStateToSnapshotMessage,
      stateFromSnapshotMessage = TerminalDayFeedArrivalActor.forecastStateFromSnapshotMessage,
      maxSnapshotInterval = maxSnapshotInterval,
    )
  }

  def live(year: Int,
           month: Int,
           day: Int,
           feedSource: FeedSource,
           maybePointInTime: Option[Long],
           now: () => Long,
           maxSnapshotInterval: Int = 250,
          ): TerminalDayFeedArrivalActor[LiveArrival] = {
    new TerminalDayFeedArrivalActor(year, month, day, T1, feedSource, maybePointInTime,
      eventToMaybeMessage = TerminalDayFeedArrivalActor.liveDiffToMaybeMessage(now),
      messageToState = TerminalDayFeedArrivalActor.liveStateFromMessage,
      stateToSnapshotMessage = TerminalDayFeedArrivalActor.liveStateToSnapshotMessage,
      stateFromSnapshotMessage = TerminalDayFeedArrivalActor.liveStateFromSnapshotMessage,
      maxSnapshotInterval = maxSnapshotInterval,
    )
  }
}

class TerminalDayFeedArrivalActor[A <: FeedArrival](year: Int,
                                                    month: Int,
                                                    day: Int,
                                                    terminal: Terminal,
                                                    feedSource: FeedSource,
                                                    override val maybePointInTime: Option[Long],
                                                    override val eventToMaybeMessage: PartialFunction[(Any, Map[UniqueArrival, A]), Option[GeneratedMessage]],
                                                    override val messageToState: (GeneratedMessage, Map[UniqueArrival, A]) => Map[UniqueArrival, A],
                                                    override val stateToSnapshotMessage: Map[UniqueArrival, A] => GeneratedMessage,
                                                    override val stateFromSnapshotMessage: GeneratedMessage => Map[UniqueArrival, A],
                                                    override val maxSnapshotInterval: Int = 250,
                                                   ) extends PartitionActor[Map[UniqueArrival, A], Query] {
  override def persistenceId: String = f"${feedSource.id}-feed-arrivals-${terminal.toString.toLowerCase}-$year-$month%02d-$day%02d"

  override def emptyState: Map[UniqueArrival, A] = Map.empty

  override val maybeMessageToMaybeAck: Option[GeneratedMessage] => Option[Any] =
    maybeMsg => Option(maybeMsg.nonEmpty)

  override lazy val maybeSnapshotInterval: Option[Int] = Option(maxSnapshotInterval)
  override val processQuery: PartialFunction[Any, Unit] = {
    case GetState => sender() ! state
  }
}
