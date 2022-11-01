package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.ports.PaxTypes.{Transit, VisaNational}
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.{ApiSplitsWithHistoricalEGateAndFTPercentages, Historical}
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitSource, SplitSources}
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.splits.ApiSplitsToSplitRatio

class ApiFlightWithSplitsSpec extends Specification {
  "A flight with splits" should {
    val scheduledAfterPaxSources = 1655247600000L
    val scheduledBeforePaxSources = scheduledAfterPaxSources - 1000
    "have valid Api when api splits pax count is within the 5% Threshold of LiveSourceFeed pax count" in {
      "and there are no transfer pax" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 0, 41, 0, Set(LiveFeedSource),
          Set(TotalPaxSource(Option(40), LiveFeedSource)), scheduledAfterPaxSources)
        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
        flightWithSplits.isWithinThreshold(apiSplits) mustEqual true
        flightWithSplits.hasValidApi mustEqual true
      }

      "and there are transfer pax only in the port feed data" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 20, 21, 0, Set(LiveFeedSource), Set(TotalPaxSource(Option(40), LiveFeedSource)), scheduledAfterPaxSources)
        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
        flightWithSplits.isWithinThreshold(apiSplits) mustEqual true
        flightWithSplits.hasValidApi mustEqual true
      }

      "and there are transfer pax only in the API data" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 0, 41, 20, Set(LiveFeedSource), Set(TotalPaxSource(Option(40), LiveFeedSource)), scheduledAfterPaxSources)
        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
        flightWithSplits.isWithinThreshold(apiSplits) mustEqual true
        flightWithSplits.hasValidApi mustEqual true
      }

      "and there are transfer pax both in the API data and in the port feed" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 20, 21, 20, Set(LiveFeedSource), Set(TotalPaxSource(Option(40), LiveFeedSource)), scheduledAfterPaxSources)
        val apiSplits = flightWithSplits.splits.find(s => s.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
        flightWithSplits.isWithinThreshold(apiSplits) mustEqual true
        flightWithSplits.hasValidApi mustEqual true
      }

      "or there is a ScenarioSimulationSource" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(20), 0, 41, 0, Set(LiveFeedSource, ScenarioSimulationSource), Set(), scheduledAfterPaxSources)

        flightWithSplits.hasValidApi mustEqual true
      }
    }

    "not have valid Api when api splits pax count outside the 5% Threshold of LiveSourceFeed pax count" in {
      "and there are no transfer pax" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 0, 45, 0, Set(LiveFeedSource), Set(TotalPaxSource(Option(40), LiveFeedSource)), scheduledAfterPaxSources)
        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
        flightWithSplits.isWithinThreshold(apiSplits) mustEqual false
        flightWithSplits.hasValidApi mustEqual false
      }

      "and there are transfer pax only in the port feed data" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 20, 24, 0, Set(LiveFeedSource), Set(TotalPaxSource(Option(40), LiveFeedSource)), scheduledAfterPaxSources)
        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
        flightWithSplits.isWithinThreshold(apiSplits) mustEqual false
        flightWithSplits.hasValidApi mustEqual false
      }

      "and there are transfer pax only in the API data" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 0, 21, 25, Set(LiveFeedSource), Set(TotalPaxSource(Option(40), LiveFeedSource)), scheduledAfterPaxSources)
        val apiSplits = flightWithSplits.splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
        flightWithSplits.isWithinThreshold(apiSplits) mustEqual false
        flightWithSplits.hasValidApi mustEqual false
      }

      "and there are transfer pax both in the API data and in the port feed" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 20, 25, 25, Set(LiveFeedSource), Set(TotalPaxSource(Option(40), LiveFeedSource)), scheduledAfterPaxSources)
        val apiSplits = flightWithSplits.splits.find(s => s.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages).get
        flightWithSplits.isWithinThreshold(apiSplits) mustEqual false
        flightWithSplits.hasValidApi mustEqual false
      }
    }

    "not have valid Api splits when the splits source type is not ApiSplitsWithHistoricalEGateAndFTPercentages" in {
      val flightWithSplits = flightWithPaxAndHistoricSplits(Option(40), 0, 41, 0, Set(LiveFeedSource))
      flightWithSplits.hasValidApi mustEqual false
    }

    "have valid Api splits when flight has a LiveFeedSource and a Live TotalPaxSource containing no passenger numbers" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(100), 0, 40, 0, Set(LiveFeedSource), Set(TotalPaxSource(None, LiveFeedSource)), scheduledAfterPaxSources)
        flightWithSplits.hasValidApi mustEqual true
    }

    "have valid Api splits when flight has a LiveFeedSource and no Live TotalPaxSource" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(100), 0, 40, 0, Set(LiveFeedSource), Set(), scheduledAfterPaxSources)
        flightWithSplits.hasValidApi mustEqual true
    }

    "have no valid Api splits when flight scheduled before pax sources were recorded has a LiveFeedSource and ActPax more than 5% different to API pax" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(100), 0, 40, 0, Set(LiveFeedSource), Set(), scheduledBeforePaxSources)
        flightWithSplits.hasValidApi mustEqual false
    }

    "have valid Api splits when flight has no LiveFeedSource" in {
      "and pax count differences are within the threshold" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 0, 40, 0, Set(), Set(), scheduledAfterPaxSources)
        flightWithSplits.hasValidApi mustEqual true
      }
      "and pax count differences are outside the threshold" in {
        val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 0, 100, 0, Set(), Set(), scheduledAfterPaxSources)
        flightWithSplits.hasValidApi mustEqual true
      }
    }

    "when there no actual pax number in liveFeed" in {
      "and api splits has pax number and hasValidApi is true" in {
        val flightWithSplits = flightWithPaxAndApiSplits(None, 0, 100, 0, Set(LiveFeedSource), Set(TotalPaxSource(Option(40), LiveFeedSource)), scheduledAfterPaxSources)
        flightWithSplits.hasValidApi mustEqual true
        flightWithSplits.pcpPaxEstimate.pax must beSome(100)
        val paxPerQueue: Option[Map[Queues.Queue, Int]] = ApiSplitsToSplitRatio.paxPerQueueUsingBestSplitsAsRatio(flightWithSplits)
        paxPerQueue must beSome(collection.Map(Queues.NonEeaDesk -> 100))
      }
    }

    "give a pax count from splits when it has API splits" in {
      val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 0, 45, 0, Set(), Set(), scheduledAfterPaxSources)
      flightWithSplits.totalPaxFromApiExcludingTransfer.flatMap(_.pax) mustEqual Option(45)
    }

    "give a pax count from splits when it has API splits which does not include transfer pax" in {
      val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 0, 45, 20, Set(), Set(), scheduledAfterPaxSources)
      flightWithSplits.totalPaxFromApiExcludingTransfer.flatMap(_.pax) mustEqual Option(45)
    }

    "give a pax count from splits when it has API splits even when it is outside the trusted threshold" in {
      val flightWithSplits = flightWithPaxAndApiSplits(Option(40), 0, 150, 0, Set(LiveFeedSource), Set(), scheduledAfterPaxSources)
      flightWithSplits.totalPaxFromApiExcludingTransfer.flatMap(_.pax) mustEqual Option(150)
    }

    "give no pax count from splits it has no API splits" in {
      val flightWithSplits = flightWithPaxAndHistoricSplits(Option(40), 0, 45, 20, Set())
      flightWithSplits.totalPaxFromApiExcludingTransfer.flatMap(_.pax) must beNone
    }

    "give None for totalPaxFromApiExcludingTransfer when it doesn't have live API splits" in {
      val fws = flightWithPaxAndHistoricSplits(Option(100), 10, 100, 0, Set())
      fws.totalPaxFromApiExcludingTransfer === None
    }

    "give None for totalPaxFromApi when it doesn't have live API splits" in {
      val fws = flightWithPaxAndHistoricSplits(Option(100), 10, 100, 0, Set())
      fws.totalPaxFromApi === None
    }
  }

  private def flightWithPaxAndApiSplits(actPax: Option[Int],
                                        transferPax: Int,
                                        splitsDirect: Int,
                                        splitsTransfer: Int,
                                        sources: Set[FeedSource],
                                        totalPax: Set[TotalPaxSource],
                                        scheduled: Long,
                                       ): ApiFlightWithSplits = {
    val flight: Arrival = ArrivalGenerator.arrival(actPax = actPax, tranPax = Option(transferPax), feedSources = sources, totalPax = totalPax, sch = scheduled)

    ApiFlightWithSplits(flight, Set(splitsForPax(directPax = splitsDirect, transferPax = splitsTransfer, ApiSplitsWithHistoricalEGateAndFTPercentages)))
  }

  private def flightWithPaxAndHistoricSplits(actPax: Option[Int], transferPax: Int, splitsDirect: Int, splitsTransfer: Int, sources: Set[FeedSource]): ApiFlightWithSplits = {
    val flight: Arrival = ArrivalGenerator.arrival(actPax = actPax, tranPax = Option(transferPax), feedSources = sources)

    ApiFlightWithSplits(flight, Set(splitsForPax(directPax = splitsDirect, transferPax = splitsTransfer, Historical)))
  }

  def splitsForPax(directPax: Int, transferPax: Int, source: SplitSource): Splits = Splits(Set(
    ApiPaxTypeAndQueueCount(VisaNational, Queues.NonEeaDesk, directPax, None, None),
    ApiPaxTypeAndQueueCount(Transit, Queues.Transfer, transferPax, None, None),
  ), source, Option(EventTypes.DC))

}
