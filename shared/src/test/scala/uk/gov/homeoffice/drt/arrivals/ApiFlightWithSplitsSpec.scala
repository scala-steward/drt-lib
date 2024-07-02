package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.ports.PaxTypes.{Transit, VisaNational}
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.{ApiSplitsWithHistoricalEGateAndFTPercentages, Historical}
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitSource, SplitSources}
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.splits.ApiSplitsToSplitRatio

class ApiFlightWithSplitsSpec extends Specification {
//  "A flight with splits" should {
//    val scheduledAfterPaxSources = 1655247600000L
//    val scheduledBeforePaxSources = scheduledAfterPaxSources - 1000
//    "have valid Api when api splits pax count is within the 5% Threshold of LiveSourceFeed pax count" in {
//      "and there are no transfer pax" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(41, 0, Set(LiveFeedSource),
//          Map(LiveFeedSource -> Passengers(Option(40),Option(0))), scheduledAfterPaxSources)
//        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
//        apiSplits.isWithinThreshold(flightWithSplits.apiFlight.PassengerSources.get(LiveFeedSource), ApiFlightWithSplits.liveApiTolerance) mustEqual true
//        flightWithSplits.hasValidApi mustEqual true
//      }
//
//      "and there are transfer pax only in the port feed data" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(21, 0, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(Option(40),Option(20))), scheduledAfterPaxSources)
//        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
//        apiSplits.isWithinThreshold(flightWithSplits.apiFlight.PassengerSources.get(LiveFeedSource), ApiFlightWithSplits.liveApiTolerance) mustEqual true
//        flightWithSplits.hasValidApi mustEqual true
//      }
//
//      "and there are transfer pax only in the API data" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(41, 20, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(Option(40),Option(0))), scheduledAfterPaxSources)
//        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
//        apiSplits.isWithinThreshold(flightWithSplits.apiFlight.PassengerSources.get(LiveFeedSource), ApiFlightWithSplits.liveApiTolerance) mustEqual true
//        flightWithSplits.hasValidApi mustEqual true
//      }
//
//      "and there are transfer pax both in the API data and in the port feed" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(21, 20, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(Option(40),Option(20))), scheduledAfterPaxSources)
//        val apiSplits = flightWithSplits.splits.find(s => s.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
//        apiSplits.isWithinThreshold(flightWithSplits.apiFlight.PassengerSources.get(LiveFeedSource), ApiFlightWithSplits.liveApiTolerance) mustEqual true
//        flightWithSplits.hasValidApi mustEqual true
//      }
//
//      "or there is a ScenarioSimulationSource" in {
//        val flightWithSplits = flightWithPaxAndApiSplits( 41, 0, Set(LiveFeedSource, ScenarioSimulationSource), Map(LiveFeedSource->Passengers(Option(20), Option(0))), scheduledAfterPaxSources)
//        flightWithSplits.hasValidApi mustEqual true
//      }
//    }
//
//    "not have valid Api when api splits pax count outside the 5% Threshold of LiveSourceFeed pax count" in {
//      "and there are no transfer pax" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(45, 0, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(Option(40),Option(0))), scheduledAfterPaxSources)
//        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
//        apiSplits.isWithinThreshold(flightWithSplits.apiFlight.PassengerSources.get(LiveFeedSource), ApiFlightWithSplits.liveApiTolerance) mustEqual false
//        flightWithSplits.hasValidApi mustEqual false
//      }
//
//      "and there are transfer pax only in the port feed data" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(24, 0, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(Option(40),Option(20))), scheduledAfterPaxSources)
//        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
//        apiSplits.isWithinThreshold(flightWithSplits.apiFlight.PassengerSources.get(LiveFeedSource), ApiFlightWithSplits.liveApiTolerance) mustEqual false
//        flightWithSplits.hasValidApi mustEqual false
//      }
//
//      "and there are transfer pax only in the API data" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(21, 25, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(Option(40),Option(0))), scheduledAfterPaxSources)
//        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
//        apiSplits.isWithinThreshold(flightWithSplits.apiFlight.PassengerSources.get(LiveFeedSource), ApiFlightWithSplits.liveApiTolerance) mustEqual false
//        flightWithSplits.hasValidApi mustEqual false
//      }
//
//      "and there are transfer pax both in the API data and in the port feed" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(25, 25, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(Option(40),Option(20))), scheduledAfterPaxSources)
//        val apiSplits = flightWithSplits.splits.find(s => s.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
//        apiSplits.isWithinThreshold(flightWithSplits.apiFlight.PassengerSources.get(LiveFeedSource), ApiFlightWithSplits.liveApiTolerance) mustEqual false
//        flightWithSplits.hasValidApi mustEqual false
//      }
//    }
//
//    "not have valid Api splits when the splits source type is not ApiSplitsWithHistoricalEGateAndFTPercentages" in {
//      val flightWithSplits = flightWithPaxAndHistoricSplits(41, 0, Set(LiveFeedSource),Map(LiveFeedSource -> Passengers(Option(40), Option(0))))
//      flightWithSplits.hasValidApi mustEqual false
//    }
//
//    "have valid Api splits when flight has a LiveFeedSource and a Live TotalPaxSource containing no passenger numbers" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(40, 0, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(None, None)), scheduledAfterPaxSources)
//        flightWithSplits.hasValidApi mustEqual true
//    }
//
//    "have valid Api splits when flight has a LiveFeedSource and no Live TotalPaxSource" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(40, 0, Set(LiveFeedSource), Map(), scheduledAfterPaxSources)
//        flightWithSplits.hasValidApi mustEqual true
//    }
//
//    "have no valid Api splits when flight scheduled before pax sources were recorded has a LiveFeedSource and ActPax more than 5% different to API pax" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(40, 0, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(Option(100), Option(0))), scheduledBeforePaxSources)
//        flightWithSplits.hasValidApi mustEqual false
//    }
//
//    "have valid Api splits when flight has no LiveFeedSource" in {
//      "and pax count differences are within the threshold" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(0, 0, Set(), Map(), scheduledAfterPaxSources)
//        flightWithSplits.hasValidApi mustEqual true
//      }
//      "and pax count differences are outside the threshold" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(100, 0, Set(), Map(), scheduledAfterPaxSources)
//        flightWithSplits.hasValidApi mustEqual true
//      }
//    }
//
//    val sourceOrderPreference = List(
//      ScenarioSimulationSource,
//      LiveFeedSource,
//      ApiFeedSource,
//      ForecastFeedSource,
//      MlFeedSource,
//      HistoricApiFeedSource,
//      AclFeedSource,
//    )
//
//    "when there no actual pax number in liveFeed" in {
//      "and api splits has pax number and hasValidApi is true" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(100, 0, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(None,Option(0))), scheduledAfterPaxSources)
//        flightWithSplits.hasValidApi mustEqual true
//        flightWithSplits.apiFlight.bestPcpPaxEstimate(sourceOrderPreference) must beSome(100)
//        val paxPerQueue: Option[Map[Queues.Queue, Int]] = ApiSplitsToSplitRatio.paxPerQueueUsingBestSplitsAsRatio(flightWithSplits, sourceOrderPreference)
//        paxPerQueue must ===(Some(Map(Queues.NonEeaDesk -> 100)))
//      }
//    }
//
//    "when there actual pax number in liveFeed" in {
//      "and api splits has pax number and hasValidApi is false" in {
//        val flightWithSplits = flightWithPaxAndApiSplits(100, 0, Set(LiveFeedSource), Map(LiveFeedSource -> Passengers(Option(95), Option(0))), scheduledAfterPaxSources)
//        flightWithSplits.hasValidApi mustEqual false
//        flightWithSplits.apiFlight.bestPcpPaxEstimate(sourceOrderPreference) must beSome(95)
//        flightWithSplits.apiFlight.bestPaxEstimate(sourceOrderPreference).feedSource === LiveFeedSource
//        val paxPerQueue: Option[Map[Queues.Queue, Int]] = ApiSplitsToSplitRatio.paxPerQueueUsingBestSplitsAsRatio(flightWithSplits, sourceOrderPreference)
//        paxPerQueue must beNone
//      }
//    }
//
//    "give a pax count from splits when it has API splits" in {
//      val flightWithSplits = flightWithPaxAndApiSplits( 45, 0, Set(), Map(), scheduledAfterPaxSources)
//      flightWithSplits.apiFlight.bestPaxEstimate(sourceOrderPreference).passengers.actual mustEqual Option(45)
//    }
//
//    "give a pax count from splits when it has API splits which does not include transfer pax" in {
//      val flightWithSplits = flightWithPaxAndApiSplits(45, 20, Set(), Map(), scheduledAfterPaxSources)
//      flightWithSplits.apiFlight.bestPaxEstimate(sourceOrderPreference).passengers.actual mustEqual Option(45)
//    }
//
//    "give a pax count from splits when it has API splits even when it is outside the trusted threshold" in {
//      val flightWithSplits = flightWithPaxAndApiSplits( 150, 0, Set(LiveFeedSource), Map(), scheduledAfterPaxSources)
//      flightWithSplits.apiFlight.bestPaxEstimate(sourceOrderPreference).passengers.actual mustEqual Option(150)
//    }
//
//    "give no pax count from splits it has no API splits" in {
//      val flightWithSplits = flightWithPaxAndHistoricSplits( 45, 20, Set(),Map())
//      flightWithSplits.apiFlight.bestPaxEstimate(sourceOrderPreference).passengers.actual must beNone
//    }
//
//    "give None for totalPaxFromApiExcludingTransfer when it doesn't have live API splits" in {
//      val fws = flightWithPaxAndHistoricSplits( 100, 0, Set(),Map())
//      fws.apiFlight.bestPaxEstimate(sourceOrderPreference).passengers.actual === None
//    }
//
//    "give None for totalPaxFromApi when it doesn't have live API splits" in {
//      val fws = flightWithPaxAndHistoricSplits(100, 0, Set(),Map())
//      fws.apiFlight.bestPaxEstimate(sourceOrderPreference).passengers.actual === None
//    }
//  }

  "bestSplits" should {
    val flight = ApiFlightWithSplits(apiFlight = ArrivalGenerator.arrival(iata = "BA0001", passengerSources = Map(LiveFeedSource -> Passengers(Option(100), None))), splits = Set.empty, lastUpdated = None)
    val apiSplits = Splits(Set(ApiPaxTypeAndQueueCount(VisaNational, Queues.NonEeaDesk, 100, None, None)), SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages, Option(EventTypes.DC))
    val invalidApiSplits = Splits(Set(ApiPaxTypeAndQueueCount(VisaNational, Queues.NonEeaDesk, 1, None, None)), SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages, Option(EventTypes.DC))
    val validButAllTransitApi = Splits(Set(ApiPaxTypeAndQueueCount(Transit, Queues.Transfer, 100, None, None)), SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages, Option(EventTypes.DC))
    val historicalSplits = Splits(Set(ApiPaxTypeAndQueueCount(VisaNational, Queues.NonEeaDesk, 100, None, None)), SplitSources.Historical, Option(EventTypes.DC))
    val terminalSplits = Splits(Set(ApiPaxTypeAndQueueCount(VisaNational, Queues.NonEeaDesk, 100, None, None)), SplitSources.TerminalAverage, Option(EventTypes.DC))

    "return api if found and valid, and non-zero pax are not all transit" in {
      val flightWithSplits = flight.copy(splits = Set(apiSplits, historicalSplits, terminalSplits))
      flightWithSplits.bestSplits.get must be equalTo apiSplits
    }
    "return historic if api not valid and historic exists" in {
      val flightWithSplits = flight.copy(splits = Set(invalidApiSplits, historicalSplits, terminalSplits))
      flightWithSplits.bestSplits.get must be equalTo historicalSplits
    }
    "return historic if api only has transit pax and historic exists" in {
      val flightWithSplits = flight.copy(splits = Set(validButAllTransitApi, historicalSplits, terminalSplits))
      flightWithSplits.bestSplits.get must be equalTo historicalSplits
    }
  }

  private def flightWithPaxAndApiSplits(splitsDirect: Int,
                                        splitsTransfer: Int,
                                        sources: Set[FeedSource],
                                        passengerSources: Map[FeedSource, Passengers],
                                        scheduled: Long,
                                       ): ApiFlightWithSplits = {
    val flight: Arrival = ArrivalGenerator.arrival(
      feedSources = sources,
      passengerSources = passengerSources + (ApiFeedSource -> Passengers(Option(splitsDirect), Option(splitsTransfer))),
      sch = scheduled)

    ApiFlightWithSplits(flight, Set(splitsForPax(directPax = splitsDirect, transferPax = splitsTransfer, ApiSplitsWithHistoricalEGateAndFTPercentages)))
  }

  private def flightWithPaxAndHistoricSplits(splitsDirect: Int, splitsTransfer: Int, sources: Set[FeedSource], passengerSources: Map[FeedSource, Passengers]): ApiFlightWithSplits = {
    val flight: Arrival = ArrivalGenerator
      .arrival(feedSources = sources,passengerSources = passengerSources)

    ApiFlightWithSplits(flight, Set(splitsForPax(directPax = splitsDirect, transferPax = splitsTransfer, Historical)))
  }

  def splitsForPax(directPax: Int, transferPax: Int, source: SplitSource): Splits = Splits(Set(
    ApiPaxTypeAndQueueCount(VisaNational, Queues.NonEeaDesk, directPax, None, None),
    ApiPaxTypeAndQueueCount(Transit, Queues.Transfer, transferPax, None, None),
  ), source, Option(EventTypes.DC))

}
