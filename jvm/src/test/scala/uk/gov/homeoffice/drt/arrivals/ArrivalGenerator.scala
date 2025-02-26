package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.Terminals.{T1, Terminal}
import uk.gov.homeoffice.drt.ports.{FeedSource, LiveFeedSource, PortCode}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

object ArrivalGenerator {
  val midnight20220515Bst: Long = 1655247600000L

  def arrival(iata: String = "",
              schDt: String = "",
              maxPax: Option[Int] = None,
              terminal: Terminal = T1,
              origin: PortCode = PortCode("JFK"),
              previousPort: Option[PortCode] = None,
              operator: Option[Operator] = None,
              status: ArrivalStatus = ArrivalStatus(""),
              estDt: String = "",
              actDt: String = "",
              estChoxDt: String = "",
              actChoxDt: String = "",
              gate: Option[String] = None,
              stand: Option[String] = None,
              runwayId: Option[String] = None,
              baggageReclaimId: Option[String] = None,
              totalPax: Option[Int] = None,
              transPax: Option[Int] = None,
              feedSource: FeedSource,
             ): Arrival = {
    val actualArrival = live(
      iata, schDt, maxPax, terminal, origin, previousPort, operator, status, estDt, actDt, estChoxDt,
      actChoxDt, gate, stand, runwayId, baggageReclaimId, totalPax, transPax
    )
      .toArrival(feedSource)
    actualArrival.copy(PcpTime = Option(actualArrival.bestArrivalTime(true)))
  }

  def live(iata: String = "BA0001",
           schDt: String = "",
           maxPax: Option[Int] = None,
           terminal: Terminal = T1,
           origin: PortCode = PortCode("JFK"),
           previousPort: Option[PortCode] = None,
           operator: Option[Operator] = None,
           status: ArrivalStatus = ArrivalStatus(""),
           estDt: String = "",
           actDt: String = "",
           estChoxDt: String = "",
           actChoxDt: String = "",
           gate: Option[String] = None,
           stand: Option[String] = None,
           runwayId: Option[String] = None,
           baggageReclaimId: Option[String] = None,
           totalPax: Option[Int] = None,
           transPax: Option[Int] = None,
          ): LiveArrival = {
    val (carrierCode, voyageNumber, suffix) = FlightCode.flightCodeToParts(iata)

    LiveArrival(
      operator = operator.map(_.code),
      maxPax = maxPax,
      totalPax = totalPax,
      transPax = transPax,
      terminal = terminal,
      voyageNumber = voyageNumber.numeric,
      carrierCode = carrierCode.code,
      flightCodeSuffix = suffix.map(_.suffix),
      origin = origin.iata,
      previousPort = previousPort.map(_.iata),
      scheduled = if (schDt.nonEmpty) SDate(schDt).millisSinceEpoch else 0,
      estimated = if (estDt.nonEmpty) Option(SDate(estDt).millisSinceEpoch) else None,
      touchdown = if (actDt.nonEmpty) Option(SDate(actDt).millisSinceEpoch) else None,
      estimatedChox = if (estChoxDt.nonEmpty) Option(SDate(estChoxDt).millisSinceEpoch) else None,
      actualChox = if (actChoxDt.nonEmpty) Option(SDate(actChoxDt).millisSinceEpoch) else None,
      status = status.description,
      gate = gate,
      stand = stand,
      runway = runwayId,
      baggageReclaim = baggageReclaimId,
    )
  }


  def flightWithSplitsForDayAndTerminal(date: SDateLike, terminal: Terminal = T1): ApiFlightWithSplits = ApiFlightWithSplits(
    live(schDt = date.toISOString, terminal = terminal).toArrival(LiveFeedSource), Set(), Option(date.millisSinceEpoch)
  )

  def arrivalForDayAndTerminal(date: SDateLike, terminal: Terminal = T1): Arrival =
    live(schDt = date.toISOString, terminal = terminal).toArrival(LiveFeedSource)
}
