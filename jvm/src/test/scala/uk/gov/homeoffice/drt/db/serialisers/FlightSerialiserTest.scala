package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.Nationality
import uk.gov.homeoffice.drt.arrivals._
import uk.gov.homeoffice.drt.db.serialisers.FlightRowHelper.generateFlight
import uk.gov.homeoffice.drt.ports.PaxTypes.VisaNational
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.ports._

object FlightRowHelper {
  def arrival(voyageNumber: Int, scheduled: Long, origin: PortCode): Arrival = Arrival(
    Operator = None,
    CarrierCode = CarrierCode("BA"),
    VoyageNumber = VoyageNumber(voyageNumber),
    FlightCodeSuffix = Option(FlightCodeSuffix("A")),
    Status = ArrivalStatus("scheduled"),
    Estimated = Option(1L),
    Predictions = Predictions(10L, Map("pred-a" -> 1, "pred-b" -> 2)),
    Actual = Option(2L),
    EstimatedChox = Option(3L),
    ActualChox = Option(4L),
    Gate = Option("G1"),
    Stand = Option("S1"),
    MaxPax = Option(100),
    RunwayID = None,
    BaggageReclaimId = Option("B1"),
    AirportID = PortCode(""),
    Terminal = T1,
    Origin = origin,
    PreviousPort = Option(PortCode("JFK")),
    Scheduled = scheduled,
    PcpTime = Option(5L),
    FeedSources = Set(LiveFeedSource, ForecastFeedSource),
    CarrierScheduled = Option(6L),
    ScheduledDeparture = Option(7L),
    RedListPax = Option(8),
    PassengerSources = Map(
      LiveFeedSource -> Passengers(Option(100), Option(50)),
      ForecastFeedSource -> Passengers(Option(101), Option(51)),
    ),
  )
  val nats = Map(
    Nationality("GB") -> 10d,
    Nationality("FR") -> 11d,
  )
  val ages = Map(
    PaxAge(1) -> 10d,
    PaxAge(2) -> 11d,
  )
  val splits: Set[Splits] = Set(
    Splits(Set(ApiPaxTypeAndQueueCount(VisaNational, Queues.NonEeaDesk, 100, Option(nats), Option(ages))), SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages, Option(EventTypes.DC)),
    Splits(Set(ApiPaxTypeAndQueueCount(VisaNational, Queues.NonEeaDesk, 100, Option(nats), Option(ages))), SplitSources.Historical, Option(EventTypes.DC)),
  )
  def generateFlight(voyageNumber: Int, scheduled: Long, origin: PortCode): ApiFlightWithSplits = ApiFlightWithSplits(
    apiFlight = arrival(voyageNumber, scheduled, origin),
    splits = splits,
    lastUpdated = Option(123L),
  )
}

class FlightSerialiserTest extends AnyWordSpec with Matchers {
  "FlightSerialiser" should {
    "convert to and from a row" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))
      val row = FlightSerialiser.toRow(PortCode("LHR"))(flight)
      val deserialised = FlightSerialiser.fromRow(row)

      deserialised should be(flight)
    }
  }
}
