package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.arrivals.ArrivalGeneratorShared.arrival
import uk.gov.homeoffice.drt.ports.{AclFeedSource, ApiFeedSource, FeedSource, ForecastFeedSource, HistoricApiFeedSource, LiveFeedSource, MlFeedSource, ScenarioSimulationSource}

class ArrivalPcpPaxSpec extends Specification {

  private val sourceOrderPreference: List[FeedSource] = List(
    ScenarioSimulationSource,
    LiveFeedSource,
    ApiFeedSource,
    ForecastFeedSource,
    MlFeedSource,
    HistoricApiFeedSource,
    AclFeedSource,
  )

  "When calculating PCP Pax for flights with a Live Feed Source" >> {
    "Given an arrival with 100 pax from API and 50 from Act Pax " +
      "Then I should expect 50 PCP pax" >> {
      val a = arrival(feedSources = Set(LiveFeedSource),
        passengerSources = Map(LiveFeedSource -> Passengers(Option(50), None), ApiFeedSource -> Passengers(Option(100), None)))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 50

      result === expected
    }

    "Given an arrival with None from API and 50 from Act Pax " +
      "Then I should expect 50 PCP pax" >> {
      val a = arrival(feedSources = Set(LiveFeedSource),
        passengerSources = Map(LiveFeedSource -> Passengers(Option(50), None)))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 50

      result === expected
    }

    "Given an arrival with 100 pax from API and None from Act Pax " +
      "Then I should expect 100 PCP pax" >> {
      val a = arrival(feedSources = Set(LiveFeedSource),
        passengerSources = Map(ApiFeedSource -> Passengers(Option(100), None)))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 100

      result === expected
    }

    "Given an arrival with 100 pax and None for Transfer " +
      "Then I should expect 100 PCP pax" >> {
      val a = arrival(feedSources = Set(LiveFeedSource),
        passengerSources = Map(LiveFeedSource -> Passengers(Option(100), None)))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 100

      result === expected
    }

    "Given an arrival with more Transfer Pax than Act Pax and a MaxPax of 150 " +
      "Then we should get 0 PCP Pax " >> {
      val a = arrival(maxPax = Option(150), feedSources = Set(LiveFeedSource),
        passengerSources = Map(LiveFeedSource -> Passengers(Option(50), Option(100))))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 0

      result === expected
    }

    "Given an arrival with 100 pax and 0 Transfer " +
      "Then I should expect 100 PCP pax" >> {
      val a = arrival(feedSources = Set(LiveFeedSource),
        passengerSources = Map(LiveFeedSource -> Passengers(Option(100), Option(0))))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 100

      result === expected
    }

    "Given an arrival with 0 act pax, 0 Transfer and 130 Max Pax" +
      "Then I should expect 0 PCP pax" >> {
      val a = arrival(maxPax = Option(130), feedSources = Set(LiveFeedSource),
        passengerSources = Map(LiveFeedSource -> Passengers(Option(0), Option(0))))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 0

      result === expected
    }

    "Given an arrival with 100 act pax and 10 Transfer" +
      "Then I should expect 90 PCP pax" >> {
      val a = arrival(feedSources = Set(LiveFeedSource),
        passengerSources = Map(LiveFeedSource -> Passengers(Option(100), Option(10)), ApiFeedSource -> Passengers(Option(100), None)))
      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 90

      result === expected
    }

    "Given an arrival with no values set for act pax and transfer and 130 for max pax" +
      "Then I should expect 0 PCP pax" >> {
      val a = arrival(maxPax = Option(130), feedSources = Set(LiveFeedSource), passengerSources = Map(LiveFeedSource -> Passengers(None, None)))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 0

      result === expected
    }
  }

  "When calculating PCP Pax for flights without a Live Feed Source" >> {

    "Given an arrival with 100 pax from API and 50 from Act Pax " +
      "Then I should expect 100 PCP pax - API trumps ACL numbers" >> {
      val a = arrival(feedSources = Set(AclFeedSource),
        passengerSources = Map(ApiFeedSource -> Passengers(Option(100), None), AclFeedSource -> Passengers(Option(50), None)))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 100

      result === expected
    }

    "Given an arrival with None from API and 50 from Act Pax " +
      "Then I should expect 50 PCP pax" >> {
      val a = arrival(feedSources = Set(AclFeedSource),
        passengerSources = Map(AclFeedSource -> Passengers(Option(50), None)))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 50

      result === expected
    }

    "Given an arrival with 100 pax from API and None from Act Pax " +
      "Then I should expect 100 PCP pax" >> {
      val a = arrival(feedSources = Set(AclFeedSource),
        passengerSources = Map(ApiFeedSource -> Passengers(Option(100), None)))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 100

      result === expected
    }

    "Given an arrival with 100 pax and None for Transfer " +
      "Then I should expect 100 PCP pax" >> {
      val a = arrival(feedSources = Set(AclFeedSource),
        passengerSources = Map(AclFeedSource -> Passengers(Option(100), None)))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 100

      result === expected
    }

    "Given an arrival with more Transfer Pax than Act Pax and a MaxPax of 150 " +
      "Then we should get 0 PCP Pax " >> {
      val a = arrival(maxPax = Option(150), feedSources = Set(AclFeedSource),
        passengerSources = Map(AclFeedSource -> Passengers(Option(50), Option(100))))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 0

      result === expected
    }

    "Given an arrival with 100 pax and 0 Transfer " +
      "Then I should expect 100 PCP pax" >> {
      val a = arrival(feedSources = Set(AclFeedSource),
        passengerSources = Map(AclFeedSource -> Passengers(Option(100), Option(0))))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 100

      result === expected
    }

    "Given an arrival with 0 act pax, 0 Transfer and 130 Max Pax" +
      "Then I should expect 0 PCP pax" >> {
      val a = arrival(maxPax = Option(130), feedSources = Set(AclFeedSource),
        passengerSources = Map(AclFeedSource -> Passengers(Option(0), Option(0))))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 0

      result === expected
    }

    "Given an arrival with 100 act pax and 10 Transfer" +
      "Then I should expect 90 PCP pax" >> {
      val a = arrival(feedSources = Set(AclFeedSource),
        passengerSources = Map(AclFeedSource -> Passengers(Option(100), Option(10))))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 90

      result === expected
    }

    "Given an arrival with no values set for act pax and transfer and 130 for max pax" +
      "Then I should expect 0 PCP pax" >> {
      val a = arrival(maxPax = Option(130), feedSources = Set(AclFeedSource),
        passengerSources = Map(AclFeedSource -> Passengers(Option(0), None)))

      val result = a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)
      val expected = 0

      result === expected
    }
  }

}
