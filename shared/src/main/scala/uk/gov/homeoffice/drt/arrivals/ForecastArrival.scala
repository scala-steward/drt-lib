package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.{ForecastFeedSource, PortCode}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal

case class ForecastArrival(carrierCode: CarrierCode,
                           flightNumber: VoyageNumber,
                           maybeFlightCodeSuffix: Option[FlightCodeSuffix],
                           origin: PortCode,
                           terminal: Terminal,
                           scheduled: Long,
                           totalPax: Option[Int],
                           transPax: Option[Int],
                           maxPax: Option[Int],
                          ) {
  lazy val asArrival: Arrival = Arrival(
    Operator = None,
    CarrierCode = carrierCode,
    VoyageNumber = flightNumber,
    FlightCodeSuffix = maybeFlightCodeSuffix,
    Status = ArrivalStatus("Scheduled"),
    Estimated = None,
    Actual = None,
    Predictions = Predictions(0L, Map()),
    EstimatedChox = None,
    ActualChox = None,
    Gate = None,
    Stand = None,
    MaxPax = maxPax,
    ActPax = totalPax,
    TranPax = transPax,
    RunwayID = None,
    BaggageReclaimId = None,
    AirportID = origin,
    Terminal = terminal,
    Origin = origin,
    Scheduled = scheduled,
    PcpTime = None,
    FeedSources = Set(ForecastFeedSource),
    CarrierScheduled = None,
    ApiPax = None,
    ScheduledDeparture = None,
    RedListPax = None,
    TotalPax = Map(ForecastFeedSource -> totalPax)
  )
}
