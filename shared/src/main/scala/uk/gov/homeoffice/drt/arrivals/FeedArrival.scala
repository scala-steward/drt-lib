package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}

sealed trait FeedArrival extends WithUnique[UniqueArrival] with Updatable[FeedArrival] {
  val operator: Option[String]
  val maxPax: Option[Int]
  val totalPax: Option[Int]
  val transPax: Option[Int]
  val terminal: Terminal
  val voyageNumber: Int
  val carrierCode: String
  val flightCodeSuffix: Option[String]
  val origin: String
  val scheduled: Long
  lazy val unique: UniqueArrival = UniqueArrival(voyageNumber, terminal, scheduled, PortCode(origin))

  def toArrival(feedSource: FeedSource): Arrival

  def update(feedArrival: FeedArrival): FeedArrival
}

case class ForecastArrival(operator: Option[String],
                           maxPax: Option[Int],
                           totalPax: Option[Int],
                           transPax: Option[Int],
                           terminal: Terminal,
                           voyageNumber: Int,
                           carrierCode: String,
                           flightCodeSuffix: Option[String],
                           origin: String,
                           scheduled: Long,
                          ) extends FeedArrival {
  override def update(incoming: FeedArrival): FeedArrival = incoming match {
    case fa: ForecastArrival => fa
    case la: LiveArrival => la.copy(
      carrierCode = carrierCode,
    )
  }

  override def toArrival(feedSource: FeedSource): Arrival = Arrival(
    Operator = operator.map(Operator),
    CarrierCode = CarrierCode(carrierCode),
    VoyageNumber = VoyageNumber(voyageNumber),
    FlightCodeSuffix = flightCodeSuffix.map(FlightCodeSuffix),
    Status = ArrivalStatus("Scheduled"),
    Estimated = None,
    Predictions = Predictions(0L, Map()),
    Actual = None,
    EstimatedChox = None,
    ActualChox = None,
    Gate = None,
    Stand = None,
    MaxPax = maxPax,
    RunwayID = None,
    BaggageReclaimId = None,
    AirportID = PortCode(""),
    Terminal = terminal,
    Origin = PortCode(origin),
    Scheduled = scheduled,
    PcpTime = None,
    FeedSources = Set(feedSource),
    CarrierScheduled = None,
    ScheduledDeparture = None,
    RedListPax = None,
    PassengerSources = Map(feedSource -> Passengers(totalPax, transPax))
  )
}

case class LiveArrival(operator: Option[String],
                       maxPax: Option[Int],
                       totalPax: Option[Int],
                       transPax: Option[Int],
                       terminal: Terminal,
                       voyageNumber: Int,
                       carrierCode: String,
                       flightCodeSuffix: Option[String],
                       origin: String,
                       scheduled: Long,
                       estimated: Option[Long],
                       touchdown: Option[Long],
                       estimatedChox: Option[Long],
                       actualChox: Option[Long],
                       status: String,
                       gate: Option[String],
                       stand: Option[String],
                       runway: Option[String],
                       baggageReclaim: Option[String],
                      ) extends FeedArrival {
  override def update(incoming: FeedArrival): FeedArrival = incoming match {
    case fa: ForecastArrival => fa.copy(
      carrierCode = carrierCode,
    )
    case la: LiveArrival => la.copy(
      carrierCode = carrierCode,
      flightCodeSuffix = la.flightCodeSuffix.orElse(flightCodeSuffix),
      estimated = la.estimated.orElse(estimated),
      touchdown = la.touchdown.orElse(touchdown),
      estimatedChox = la.estimatedChox.orElse(estimatedChox),
      actualChox = la.actualChox.orElse(actualChox),
      gate = la.gate.orElse(gate),
      stand = la.stand.orElse(stand),
      runway = la.runway.orElse(runway),
      baggageReclaim = la.baggageReclaim.orElse(baggageReclaim),
    )
  }

  override def toArrival(feedSource: FeedSource): Arrival = Arrival(
    Operator = operator.map(Operator),
    CarrierCode = CarrierCode(carrierCode),
    VoyageNumber = VoyageNumber(voyageNumber),
    FlightCodeSuffix = flightCodeSuffix.map(FlightCodeSuffix),
    Status = ArrivalStatus(status),
    Estimated = estimated,
    Predictions = Predictions(0L, Map()),
    Actual = touchdown,
    EstimatedChox = estimatedChox,
    ActualChox = actualChox,
    Gate = gate,
    Stand = stand,
    MaxPax = maxPax,
    RunwayID = runway,
    BaggageReclaimId = baggageReclaim,
    AirportID = PortCode(""),
    Terminal = terminal,
    Origin = PortCode(origin),
    Scheduled = scheduled,
    PcpTime = None,
    FeedSources = Set(feedSource),
    CarrierScheduled = None,
    ScheduledDeparture = None,
    RedListPax = None,
    PassengerSources = Map(feedSource -> Passengers(totalPax, transPax))
  )
}
