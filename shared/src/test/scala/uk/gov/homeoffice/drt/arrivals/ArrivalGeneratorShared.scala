package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.Terminals.{T1, Terminal}
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}
import uk.gov.homeoffice.drt.time.SDateLike

object ArrivalGeneratorShared {
  val midnight20220515Bst: Long = 1655247600000L

  def arrival(iata: String = "",
              icao: String = "",
              sch: Long = midnight20220515Bst,
              maxPax: Option[Int] = None,
              terminal: Terminal = Terminal("T1"),
              origin: PortCode = PortCode(""),
              previousPort: Option[PortCode] = None,
              operator: Option[Operator] = None,
              status: ArrivalStatus = ArrivalStatus(""),
              predictions: Predictions = Predictions(0L, Map()),
              est: Long = 0L,
              act: Long = 0L,
              estChox: Long = 0L,
              actChox: Long = 0L,
              gate: Option[String] = None,
              stand: Option[String] = None,
              runwayId: Option[String] = None,
              baggageReclaimId: Option[String] = None,
              airportId: PortCode = PortCode(""),
              feedSources: Set[FeedSource] = Set(),
              pcpTime: Option[Long] = None,
              passengerSources: Map[FeedSource, Passengers] = Map.empty
             ): Arrival =
    Arrival(
      Operator = operator,
      Status = status,
      Predictions = predictions,
      Estimated = if (est != 0L) Option(est) else None,
      Actual = if (act != 0L) Option(act) else None,
      EstimatedChox = if (estChox != 0L) Option(estChox) else None,
      ActualChox = if (actChox != 0L) Option(actChox) else None,
      Gate = gate,
      Stand = stand,
      MaxPax = maxPax,
      RunwayID = runwayId,
      BaggageReclaimId = baggageReclaimId,
      AirportID = airportId,
      Terminal = terminal,
      rawICAO = icao,
      rawIATA = iata,
      Origin = origin,
      PreviousPort = previousPort,
      PcpTime = if (pcpTime.isDefined) Option(pcpTime.get) else if (sch != 0L) Some(sch) else None,
      Scheduled = sch,
      FeedSources = feedSources,
      PassengerSources = passengerSources
    )

  def flightWithSplitsForDayAndTerminal(date: SDateLike, terminal: Terminal = T1): ApiFlightWithSplits = ApiFlightWithSplits(
    arrival(sch = date.millisSinceEpoch, terminal = terminal), Set(), Option(date.millisSinceEpoch)
  )

  def arrivalForDayAndTerminal(date: SDateLike, terminal: Terminal = T1): Arrival =
    arrival(sch = date.millisSinceEpoch, terminal = terminal)
}
