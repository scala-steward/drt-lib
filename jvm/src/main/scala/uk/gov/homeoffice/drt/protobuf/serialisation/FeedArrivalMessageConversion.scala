package uk.gov.homeoffice.drt.protobuf.serialisation

import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.arrivals.{FlightCode, ForecastArrival, LiveArrival}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.protobuf.messages.FeedArrivalsMessage.{FeedArrivalStateSnapshotMessage, ForecastArrivalStateSnapshotMessage, ForecastFeedArrivalMessage, LiveArrivalStateSnapshotMessage, LiveFeedArrivalMessage}
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage.{FlightMessage, FlightStateSnapshotMessage}
import uk.gov.homeoffice.drt.protobuf.serialisation.FlightMessageConversion.getPassengerSources

object FeedArrivalMessageConversion {
  def forecastArrivalsFromSnapshot(msg: GeneratedMessage): Seq[ForecastArrival] =
    msg match {
      case FlightStateSnapshotMessage(flights, _) => flights.map(forecastArrivalFromLegacyMessage)
      case msg: FeedArrivalStateSnapshotMessage => FeedArrivalMessageConversion.forecastArrivalsFromSnapshotMessage(msg)
    }

  def forecastArrivalsToSnapshot(arrivals: Seq[ForecastArrival]): ForecastArrivalStateSnapshotMessage =
    ForecastArrivalStateSnapshotMessage(arrivals.map(forecastArrivalToMessage))

  def liveArrivalsToSnapshot(arrivals: Seq[LiveArrival]): LiveArrivalStateSnapshotMessage =
    LiveArrivalStateSnapshotMessage(arrivals.map(liveArrivalToMessage))

  def liveArrivalsFromSnapshot(msg: GeneratedMessage): Seq[LiveArrival] =
    msg match {
      case FlightStateSnapshotMessage(flights, _) => flights.map(liveArrivalFromLegacyMessage)
      case msg: FeedArrivalStateSnapshotMessage => FeedArrivalMessageConversion.liveArrivalsFromSnapshot(msg)
    }

  def forecastArrivalsFromSnapshotMessage(msg: FeedArrivalStateSnapshotMessage): Seq[ForecastArrival] =
    msg.forecastArrivalMessages.map(forecastArrivalFromMessage)

  def liveArrivalsFromSnapshotMessage(msg: FeedArrivalStateSnapshotMessage): Seq[LiveArrival] =
    msg.liveArrivalMessages.map(liveArrivalFromMessage)

  def forecastArrivalToMessage(fa: ForecastArrival): ForecastFeedArrivalMessage =
    ForecastFeedArrivalMessage(
      operator = Option(fa.operator),
      maxPax = fa.maxPax,
      totalPax = fa.totalPax,
      terminal = Option(fa.terminal.toString),
      voyageNumber = Option(fa.voyageNumber),
      carrierCode = Option(fa.carrierCode),
      flightCodeSuffix = fa.flightCodeSuffix,
      origin = Option(fa.origin),
      scheduled = Option(fa.scheduled)
    )

  def forecastArrivalFromMessage(msg: ForecastFeedArrivalMessage): ForecastArrival =
    ForecastArrival(
      operator = msg.operator.getOrElse(""),
      maxPax = msg.maxPax,
      totalPax = msg.totalPax,
      transPax = msg.transPax,
      terminal = Terminal(msg.terminal.getOrElse("")),
      voyageNumber = msg.voyageNumber.getOrElse(0),
      carrierCode = msg.carrierCode.getOrElse(""),
      flightCodeSuffix = msg.flightCodeSuffix,
      origin = msg.origin.getOrElse(""),
      scheduled = msg.scheduled.getOrElse(0L),
    )

  def liveArrivalToMessage(fa: LiveArrival): LiveFeedArrivalMessage =
    LiveFeedArrivalMessage(
      operator = Option(fa.operator),
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

  def liveArrivalFromMessage(msg: LiveFeedArrivalMessage): LiveArrival =
    LiveArrival(
      operator = msg.operator.getOrElse(""),
      maxPax = msg.maxPax,
      totalPax = msg.totalPax,
      transPax = msg.transPax,
      terminal = Terminal(msg.terminal.getOrElse("")),
      voyageNumber = msg.voyageNumber.getOrElse(0),
      carrierCode = msg.carrierCode.getOrElse(""),
      flightCodeSuffix = msg.flightCodeSuffix,
      origin = msg.origin.getOrElse(""),
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

  def forecastArrivalFromLegacyMessage(msg: FlightMessage): ForecastArrival = {
    val (totalPax, transPax) = getPassengerSources(msg).values.headOption.map(p => (p.actual, p.transit)).getOrElse((None, None))
    val bestFlightCode = if (msg.iCAO.exists(_.nonEmpty)) msg.iCAO.getOrElse("") else msg.iCAO.getOrElse("")
    val (carrierCode, voyageNumber, suffix) = FlightCode.flightCodeToParts(bestFlightCode)

    ForecastArrival(
      operator = msg.operator.getOrElse(""),
      maxPax = msg.maxPax,
      totalPax = totalPax,
      transPax = transPax,
      terminal = Terminal(msg.terminal.getOrElse("")),
      voyageNumber = voyageNumber.numeric,
      carrierCode = carrierCode.code,
      flightCodeSuffix = suffix.map(_.suffix),
      origin = msg.origin.getOrElse(""),
      scheduled = msg.scheduled.getOrElse(0L),
    )
  }

  def liveArrivalFromLegacyMessage(msg: FlightMessage): LiveArrival = {
    val (totalPax, transPax) = getPassengerSources(msg).values.headOption.map(p => (p.actual, p.transit)).getOrElse((None, None))
    val bestFlightCode = if (msg.iCAO.exists(_.nonEmpty)) msg.iCAO.getOrElse("") else msg.iCAO.getOrElse("")
    val (carrierCode, voyageNumber, suffix) = FlightCode.flightCodeToParts(bestFlightCode)

    LiveArrival(
      operator = msg.operator.getOrElse(""),
      maxPax = msg.maxPax,
      totalPax = totalPax,
      transPax = transPax,
      terminal = Terminal(msg.terminal.getOrElse("")),
      voyageNumber = voyageNumber.numeric,
      carrierCode = carrierCode.code,
      flightCodeSuffix = suffix.map(_.suffix),
      origin = msg.origin.getOrElse(""),
      scheduled = msg.scheduled.getOrElse(0L),
      estimated = msg.estimated,
      touchdown = msg.touchdown,
      estimatedChox = msg.estimatedChox,
      actualChox = msg.actualChox,
      status = msg.status.getOrElse(""),
      gate = msg.gate,
      stand = msg.stand,
      runway = msg.runwayID,
      baggageReclaim = msg.baggageReclaimId,
    )
  }
}
