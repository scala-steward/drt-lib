package uk.gov.homeoffice.drt.protobuf.serialisation

import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.TerminalDayFeedArrivalActor.FeedArrivalsDiff
import uk.gov.homeoffice.drt.arrivals.{FeedArrival, ForecastArrival, LiveArrival, UniqueArrival}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.protobuf.messages.FeedArrivalsMessage.{ForecastArrivalStateSnapshotMessage, ForecastFeedArrivalMessage, ForecastFeedArrivalsDiffMessage, LiveArrivalStateSnapshotMessage, LiveFeedArrivalMessage, LiveFeedArrivalsDiffMessage}
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage.UniqueArrivalMessage
import uk.gov.homeoffice.drt.protobuf.serialisation.FlightMessageConversion.{log, uniqueArrivalToMessage}

object FeedArrivalMessageConversion {

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

  def forecastArrivalsToMaybeDiffMessage(now: () => Long,
                                         processRemovals: Boolean,
                                        ): PartialFunction[(Any, Map[UniqueArrival, ForecastArrival]), Option[GeneratedMessage]] =
    diffToMaybeMessage(
      now,
      arrivalsToMessages(FeedArrivalMessageConversion.forecastArrivalToMessage),
      (n, u, msgs) => ForecastFeedArrivalsDiffMessage(Option(n), msgs, u),
      processRemovals
    )

  def liveArrivalsToMaybeDiffMessage(now: () => Long,
                                     processRemovals: Boolean,
                                    ): PartialFunction[(Any, Map[UniqueArrival, LiveArrival]), Option[GeneratedMessage]] =
    diffToMaybeMessage(
      now,
      arrivalsToMessages(FeedArrivalMessageConversion.liveArrivalToMessage),
      (n, u, msgs) => LiveFeedArrivalsDiffMessage(Option(n), msgs, u),
      processRemovals
    )

  private def diffToMaybeMessage[A <: FeedArrival, U](now: () => Long,
                                                      arrivalsToMessages: (Seq[A], Map[UniqueArrival, A]) => U,
                                                      toMessage: (Long, U, Seq[UniqueArrivalMessage]) => GeneratedMessage,
                                                      processRemovals: Boolean,
                                                     ): PartialFunction[(Any, Map[UniqueArrival, A]), Option[GeneratedMessage]] = {
    case (arrivals: Seq[A], state) =>
      val diff = createDiff(arrivals, state, processRemovals)
      val updatesForDiff = arrivalsToMessages(arrivals, state)
      val removalsForDiff = diff.removals.map(uniqueArrivalToMessage).toSeq

      if (diff.nonEmpty)
        Option(toMessage(now(), updatesForDiff, removalsForDiff))
      else
        None

    case unexpected =>
      log.error(s"Unexpected message: $unexpected")
      None
  }

  private def arrivalsToMessages[A <: FeedArrival, M](arrivalToMessage: A => M)
                                                     (arrivals: Seq[A],
                                                      state: Map[UniqueArrival, A],
                                                     ): Seq[M] =
    arrivals.foldLeft(Seq.empty[M]) {
      case (acc, a) =>
        state.get(a.unique) match {
          case Some(existing) if existing == a =>
            acc
          case _ =>
            acc :+ arrivalToMessage(a)
        }
    }

  private def createDiff[A <: FeedArrival](arrivals: Seq[A], state: Map[UniqueArrival, A], processRemovals: Boolean): FeedArrivalsDiff[A] = {
    val updates = arrivals.filterNot(a => state.get(a.unique).contains(a))
    val removals = if (processRemovals) state.keySet -- arrivals.map(_.unique) else Set.empty
    FeedArrivalsDiff(updates, removals)
  }

  def forecastArrivalToMessage(fa: ForecastArrival): ForecastFeedArrivalMessage =
    ForecastFeedArrivalMessage(
      operator = fa.operator,
      maxPax = fa.maxPax,
      totalPax = fa.totalPax,
      transPax = fa.transPax,
      terminal = Option(fa.terminal.toString),
      voyageNumber = Option(fa.voyageNumber),
      carrierCode = Option(fa.carrierCode),
      flightCodeSuffix = fa.flightCodeSuffix,
      origin = Option(fa.origin),
      scheduled = Option(fa.scheduled)
    )

  private def forecastArrivalFromMessage(msg: ForecastFeedArrivalMessage): ForecastArrival =
    ForecastArrival(
      operator = msg.operator,
      maxPax = msg.maxPax,
      totalPax = msg.totalPax,
      transPax = msg.transPax,
      terminal = Terminal(msg.terminal.getOrElse("")),
      voyageNumber = msg.voyageNumber.getOrElse(0),
      carrierCode = msg.carrierCode.getOrElse(""),
      flightCodeSuffix = msg.flightCodeSuffix,
      origin = msg.origin.getOrElse(""),
      previousPort = msg.previousPort,
      scheduled = msg.scheduled.getOrElse(0L),
    )

  def liveArrivalToMessage(fa: LiveArrival): LiveFeedArrivalMessage =
    LiveFeedArrivalMessage(
      operator = fa.operator,
      maxPax = fa.maxPax,
      totalPax = fa.totalPax,
      transPax = fa.transPax,
      terminal = Option(fa.terminal.toString),
      voyageNumber = Option(fa.voyageNumber),
      carrierCode = Option(fa.carrierCode),
      flightCodeSuffix = fa.flightCodeSuffix,
      origin = Option(fa.origin),
      scheduled = Option(fa.scheduled),
      estimated = fa.estimated,
      touchdown = fa.touchdown,
      estimatedChox = fa.estimatedChox,
      actualChox = fa.actualChox,
      status = Option(fa.status),
      gate = fa.gate,
      stand = fa.stand,
      runway = fa.runway,
      baggageReclaim = fa.baggageReclaim,
    )

  private def liveArrivalFromMessage(msg: LiveFeedArrivalMessage): LiveArrival =
    LiveArrival(
      operator = msg.operator,
      maxPax = msg.maxPax,
      totalPax = msg.totalPax,
      transPax = msg.transPax,
      terminal = Terminal(msg.terminal.getOrElse("")),
      voyageNumber = msg.voyageNumber.getOrElse(0),
      carrierCode = msg.carrierCode.getOrElse(""),
      flightCodeSuffix = msg.flightCodeSuffix,
      origin = msg.origin.getOrElse(""),
      previousPort = msg.previousPort,
      scheduled = msg.scheduled.getOrElse(0L),
      estimated = msg.estimated,
      touchdown = msg.touchdown,
      estimatedChox = msg.estimatedChox,
      actualChox = msg.actualChox,
      status = msg.status.getOrElse(""),
      gate = msg.gate,
      stand = msg.stand,
      runway = msg.runway,
      baggageReclaim = msg.baggageReclaim,
    )

  private def forecastArrivalsToSnapshot(arrivals: Seq[ForecastArrival]): ForecastArrivalStateSnapshotMessage =
    ForecastArrivalStateSnapshotMessage(arrivals.map(forecastArrivalToMessage))

  private def liveArrivalsToSnapshot(arrivals: Seq[LiveArrival]): LiveArrivalStateSnapshotMessage =
    LiveArrivalStateSnapshotMessage(arrivals.map(liveArrivalToMessage))
}
