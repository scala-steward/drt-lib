package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.Terminals.{T1, Terminal}
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}
import uk.gov.homeoffice.drt.time.SDateLike

import scala.collection.SortedSet

object ArrivalGenerator {
  def arrival(
               iata: String = "",
               icao: String = "",
               sch: Long = 0L,
               actPax: Option[Int] = None,
               maxPax: Option[Int] = None,
               terminal: Terminal = Terminal("T1"),
               origin: PortCode = PortCode(""),
               operator: Option[Operator] = None,
               status: ArrivalStatus = ArrivalStatus(""),
               predDt: Option[Prediction[Long]] = None,
               est: Long = 0L,
               act: Long = 0L,
               estChox: Long = 0L,
               actChox: Long = 0L,
               gate: Option[String] = None,
               stand: Option[String] = None,
               tranPax: Option[Int] = None,
               runwayId: Option[String] = None,
               baggageReclaimId: Option[String] = None,
               airportId: PortCode = PortCode(""),
               feedSources: Set[FeedSource] = Set(),
               pcpTime: Option[Long] = None,
               totalPax: Set[TotalPaxSource] = Set.empty[TotalPaxSource]
             ): Arrival =
    Arrival(
      Operator = operator,
      Status = status,
      PredictedTouchdown = predDt,
      Estimated = if (est != 0L) Option(est) else None,
      Actual = if (act != 0L) Option(act) else None,
      EstimatedChox = if (estChox != 0L) Option(estChox) else None,
      ActualChox = if (actChox != 0L) Option(actChox) else None,
      Gate = gate,
      Stand = stand,
      MaxPax = maxPax,
      ActPax = actPax,
      TranPax = tranPax,
      RunwayID = runwayId,
      BaggageReclaimId = baggageReclaimId,
      AirportID = airportId,
      Terminal = terminal,
      rawICAO = icao,
      rawIATA = iata,
      Origin = origin,
      PcpTime = if (pcpTime.isDefined) Option(pcpTime.get) else if (sch != 0L) Some(sch) else None,
      Scheduled = sch,
      FeedSources = feedSources,
      TotalPax = totalPax
    )

  def flightWithSplitsForDayAndTerminal(date: SDateLike, terminal: Terminal = T1): ApiFlightWithSplits = ApiFlightWithSplits(
    arrival(sch = date.millisSinceEpoch, terminal = terminal), Set(), Option(date.millisSinceEpoch)
  )

  def arrivalForDayAndTerminal(date: SDateLike, terminal: Terminal = T1): Arrival =
    arrival(sch = date.millisSinceEpoch, terminal = terminal)
}
