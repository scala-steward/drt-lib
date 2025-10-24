package uk.gov.homeoffice.drt.actor

import org.apache.pekko.actor.Props
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.DataUpdates.FlightUpdates
import uk.gov.homeoffice.drt.actor.TerminalDayFeedArrivalActor.{GetState, Query}
import uk.gov.homeoffice.drt.arrivals._
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{AclFeedSource, FeedSource, ForecastFeedSource}
import uk.gov.homeoffice.drt.protobuf.serialisation.FeedArrivalMessageConversion

import scala.concurrent.ExecutionContext


object TerminalDayFeedArrivalActor {
  trait Query

  trait Event

  object GetState extends Query

  case class FeedArrivalsDiff[A <: FeedArrival](updates: Iterable[A], removals: Iterable[UniqueArrival]) extends Event with FlightUpdates {
    lazy val isEmpty: Boolean = updates.isEmpty && removals.isEmpty
    lazy val nonEmpty: Boolean = !isEmpty
  }

  private def forecast(processRemovals: Boolean)
                      (year: Int,
                       month: Int,
                       day: Int,
                       terminal: Terminal,
                       feedSource: FeedSource,
                       maybePointInTime: Option[Long],
                       now: () => Long,
                       maxSnapshotInterval: Int = 250,
                      ): Props = {
    Props(new TerminalDayFeedArrivalActor(year, month, day, terminal, feedSource, maybePointInTime,
      eventToMaybeMessage = FeedArrivalMessageConversion.forecastArrivalsToMaybeDiffMessage(now, processRemovals),
      messageToState = FeedArrivalMessageConversion.forecastStateFromMessage,
      stateToSnapshotMessage = FeedArrivalMessageConversion.forecastStateToSnapshotMessage,
      stateFromSnapshotMessage = FeedArrivalMessageConversion.forecastStateFromSnapshotMessage,
      maxSnapshotInterval = maxSnapshotInterval,
    ))
  }

  private def live(year: Int,
                   month: Int,
                   day: Int,
                   terminal: Terminal,
                   feedSource: FeedSource,
                   maybePointInTime: Option[Long],
                   now: () => Long,
                   maxSnapshotInterval: Int = 250,
                  ): Props = {
    Props(new TerminalDayFeedArrivalActor(year, month, day, terminal, feedSource, maybePointInTime,
      eventToMaybeMessage = FeedArrivalMessageConversion.liveArrivalsToMaybeDiffMessage(now, processRemovals = false),
      messageToState = FeedArrivalMessageConversion.liveStateFromMessage,
      stateToSnapshotMessage = FeedArrivalMessageConversion.liveStateToSnapshotMessage,
      stateFromSnapshotMessage = FeedArrivalMessageConversion.liveStateFromSnapshotMessage,
      maxSnapshotInterval = maxSnapshotInterval,
    ))
  }

  def props(year: Int,
            month: Int,
            day: Int,
            terminal: Terminal,
            feedSource: FeedSource,
            maybePointInTime: Option[Long],
            now: () => Long,
            maxSnapshotInterval: Int = 250,
           ): Props =
    if (feedSource == AclFeedSource)
      forecast(processRemovals = true)(year, month, day, terminal, feedSource, maybePointInTime, now, maxSnapshotInterval)
    else if (feedSource == ForecastFeedSource)
      forecast(processRemovals = false)(year, month, day, terminal, feedSource, maybePointInTime, now, maxSnapshotInterval)
    else
      live(year, month, day, terminal, feedSource, maybePointInTime, now, maxSnapshotInterval)
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
                                                   ) extends PartitionActor[Map[UniqueArrival, A]] {
  implicit val ec: ExecutionContext = context.dispatcher
  
  override def persistenceId: String = f"${feedSource.id}-feed-arrivals-${terminal.toString.toLowerCase}-$year-$month%02d-$day%02d"

  override def emptyState: Map[UniqueArrival, A] = Map.empty

  override val maybeMessageToMaybeAck: Option[GeneratedMessage] => Option[Any] =
    maybeMsg => Option(maybeMsg.nonEmpty)

  override lazy val maybeSnapshotInterval: Option[Int] = Option(maxSnapshotInterval)
  override val processQuery: PartialFunction[Any, Unit] = {
    case GetState => sender() ! state
  }
}
