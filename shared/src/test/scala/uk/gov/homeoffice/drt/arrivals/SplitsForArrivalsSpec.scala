package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.arrivals.SplitStyle.PaxNumbers
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk, NonEeaDesk}
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.{ApiSplitsWithHistoricalEGateAndFTPercentages, Historical}
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.ports._

class SplitsForArrivalsSpec extends Specification {

  val now: Long = 10L
  val scheduled = 1655247600000L
  val uniqueArrival: UniqueArrival = UniqueArrival(1, T1, scheduled, PortCode("JFK"))

  "When I diff SplitsForArrivals with a FlightsWithSplits" >> {
    "Given one new split and no existing ones" >> {
      "Then I should get a SplitsForArrivals containing the new split" >> {
        val splitsForArrivals = SplitsForArrivals(Map(uniqueArrival -> Set(Splits(Set(), Historical, None, PaxNumbers))))

        val diff = splitsForArrivals.diff(Map())

        diff === splitsForArrivals
      }
    }

    "Given one new split and one existing split with the same values as the new one" >> {
      "Then I should get an empty diff" >> {
        val existingSplits = Set(Splits(Set(ApiPaxTypeAndQueueCount(EeaMachineReadable, EGate, 1, None, None)), Historical, None, PaxNumbers))
        val splitsForArrivals = SplitsForArrivals(Map(uniqueArrival -> existingSplits))

        val diff = splitsForArrivals.diff(Map(uniqueArrival -> existingSplits))

        diff === SplitsForArrivals.empty
      }
    }

    "Given one new split and one existing split of the same source but new numbers" >> {
      "Then I should get a SplitsForArrivals containing updated split" >> {
        val existingSplits = Set(Splits(Set(ApiPaxTypeAndQueueCount(EeaMachineReadable, EGate, 1, None, None)), Historical, None, PaxNumbers))
        val newSplits = Set(Splits(Set(ApiPaxTypeAndQueueCount(EeaNonMachineReadable, EeaDesk, 1, None, None)), Historical, None, PaxNumbers))
        val splitsForArrivals = SplitsForArrivals(Map(uniqueArrival -> newSplits))

        val diff = splitsForArrivals.diff(Map(uniqueArrival -> existingSplits))

        diff === SplitsForArrivals(Map(uniqueArrival -> newSplits))
      }
    }

    "Given one new split and one existing split from a different source" >> {
      "Then I should get a SplitsForArrivals containing just the new split" >> {
        val existingSplits = Set(Splits(Set(ApiPaxTypeAndQueueCount(EeaMachineReadable, EGate, 1, None, None)), Historical, None, PaxNumbers))
        val newSplits = Set(Splits(Set(ApiPaxTypeAndQueueCount(EeaNonMachineReadable, EeaDesk, 1, None, None)), ApiSplitsWithHistoricalEGateAndFTPercentages, None, PaxNumbers))
        val splitsForArrivals = SplitsForArrivals(Map(uniqueArrival -> newSplits))

        val diff = splitsForArrivals.diff(Map(uniqueArrival -> existingSplits))

        diff === SplitsForArrivals(Map(uniqueArrival -> newSplits))
      }
    }
  }

  "When I apply SplitsForArrivals to FlightsWithSplits" >> {
    "Given one new split and one matching arrival with 2 existing splits, one from the same source" >> {
      "Then I should get a FlightsWithSplits containing the matching arrival with the newly updated split along with the other pre-existing split" >> {
        val existingSplits1 = Splits(Set(ApiPaxTypeAndQueueCount(EeaMachineReadable, EGate, 1, None, None)), Historical, None, PaxNumbers)
        val existingSplits2 = Splits(Set(ApiPaxTypeAndQueueCount(VisaNational, EGate, 1, None, None)), ApiSplitsWithHistoricalEGateAndFTPercentages, None, PaxNumbers)
        val newSplits = Splits(Set(ApiPaxTypeAndQueueCount(EeaNonMachineReadable, EeaDesk, 1, None, None)), ApiSplitsWithHistoricalEGateAndFTPercentages, None, PaxNumbers)
        val splitsForArrivals = SplitsForArrivals(Map(uniqueArrival -> Set(newSplits)))
        val arrival = ArrivalGeneratorShared.arrival(iata = "BA0001", terminal = T1, origin = PortCode("JFK"),
          passengerSources = Map(LiveFeedSource -> Passengers(Option(1), Some(0)), ApiFeedSource -> Passengers(Option(1), None)), feedSources = Set(LiveFeedSource, ApiFeedSource))
        val flights = FlightsWithSplits(Seq(ApiFlightWithSplits(arrival, Set(existingSplits1, existingSplits2))))

        val updated = splitsForArrivals.applyTo(flights, now, List())._1

        updated === FlightsWithSplits(Seq(ApiFlightWithSplits(arrival.copy(
          FeedSources = Set(ApiFeedSource, LiveFeedSource),
          PassengerSources = Map(ApiFeedSource -> Passengers(Option(1), Option(0)), LiveFeedSource -> Passengers(Option(1), Option(0)))),
          Set(newSplits, existingSplits1), Option(now))))
      }
    }

    "Given one new split and arrival that matches on the last port field rather than the origin" >> {
      "Then I should get a FlightsWithSplits containing the matching arrival with the new split" >> {
        val newSplits = Splits(Set(ApiPaxTypeAndQueueCount(EeaNonMachineReadable, EeaDesk, 1, None, None)), ApiSplitsWithHistoricalEGateAndFTPercentages, None, PaxNumbers)
        val arrival = ArrivalGeneratorShared.arrival(iata = "BA0001", terminal = T1, origin = PortCode("CDG"), previousPort = Option(PortCode("JFK")),
          passengerSources = Map(LiveFeedSource -> Passengers(Option(1), Some(0)), ApiFeedSource -> Passengers(Option(1), None)), feedSources = Set(LiveFeedSource, ApiFeedSource))
        val splitsForArrivals = SplitsForArrivals(Map(uniqueArrival -> Set(newSplits)))
        val flights = FlightsWithSplits(Seq(ApiFlightWithSplits(arrival, Set())))

        val updated = splitsForArrivals.applyTo(flights, now, List())._1

        updated === FlightsWithSplits(Seq(ApiFlightWithSplits(arrival.copy(
          FeedSources = Set(ApiFeedSource, LiveFeedSource),
          PassengerSources = Map(ApiFeedSource -> Passengers(Option(1), Option(0)), LiveFeedSource -> Passengers(Option(1), Option(0)))),
          Set(newSplits), Option(now))))
      }
    }

    "Given 2 splits and two arrivals, one with a new source and one with the same source" >> {
      "Then I should get a FlightsWithSplits containing the arrivals updated with the correct new splits" >> {
        val arrival1 = ArrivalGeneratorShared.arrival(iata = "BA0001", terminal = T1, origin = PortCode("JFK"), passengerSources = Map(LiveFeedSource -> Passengers(Option(1), Some(0))))
        val arrival2 = ArrivalGeneratorShared.arrival(iata = "FR1234", terminal = T1, origin = PortCode("JFK"), passengerSources = Map(LiveFeedSource -> Passengers(Option(1), Some(0))))
        val existingSplits1 = Splits(Set(ApiPaxTypeAndQueueCount(VisaNational, NonEeaDesk, 1, None, None)), Historical, None, PaxNumbers)
        val existingSplits2 = Splits(Set(ApiPaxTypeAndQueueCount(EeaMachineReadable, EGate, 1, None, None)), Historical, None, PaxNumbers)
        val newSplits1 = Splits(Set(ApiPaxTypeAndQueueCount(EeaNonMachineReadable, EeaDesk, 1, None, None)), Historical, None, PaxNumbers)
        val newSplits2 = Splits(Set(ApiPaxTypeAndQueueCount(EeaNonMachineReadable, EeaDesk, 1, None, None)), ApiSplitsWithHistoricalEGateAndFTPercentages, None, PaxNumbers)

        val splitsForArrivals = SplitsForArrivals(Map(arrival1.unique -> Set(newSplits1), arrival2.unique -> Set(newSplits2)))

        val flights = FlightsWithSplits(Seq(
          ApiFlightWithSplits(arrival1, Set(existingSplits1)),
          ApiFlightWithSplits(arrival2, Set(existingSplits2))
        ))

        val updated = splitsForArrivals.applyTo(flights, now, List())._1

        val arrival2WithApiSources = arrival2.copy(
          FeedSources = arrival2.FeedSources + ApiFeedSource,
          PassengerSources = arrival2.PassengerSources + (ApiFeedSource -> Passengers(Option(1), Some(0)))
        )

        updated === FlightsWithSplits(Seq(
          ApiFlightWithSplits(arrival1, Set(newSplits1), Option(now)),
          ApiFlightWithSplits(arrival2WithApiSources, Set(existingSplits2, newSplits2), Option(now)),
        ))
      }
    }
  }
}
