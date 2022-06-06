package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.{ApiSplitsWithHistoricalEGateAndFTPercentages, Historical}
import uk.gov.homeoffice.drt.ports.{AclFeedSource, ApiFeedSource, ForecastFeedSource, LiveFeedSource}

class ArrivalSpec extends Specification {
  "Arrival bestPcpPaxEstimate" should {
    val arrivalBase = ArrivalGenerator.arrival()
    val liveFeedTotalPaxSource = TotalPaxSource(10, LiveFeedSource, None)
    val portForecastFeedTotalPaxSource = TotalPaxSource(10, ForecastFeedSource, None)
    val apiFeedWithSplitsTotalPaxSource = TotalPaxSource(10, ApiFeedSource, Some(ApiSplitsWithHistoricalEGateAndFTPercentages))
    val apiFeedWithHistoricalSplitsTotalPaxSource = TotalPaxSource(10, ApiFeedSource, Some(Historical))
    val apiFeedWithOutSplitsTotalPaxSource = TotalPaxSource(10, ApiFeedSource, None)
    val aclFeedTotalPaxSource = TotalPaxSource(10, AclFeedSource, None)

    "Give LiveFeedSource as bestPcpPaxEstimate, When LiveFeedSource and ApiFeedSource with ApiSplitsWithHistoricalEGateAndFTPercentages is present in total pax" in {
      val arrival = arrivalBase.copy(TotalPax = Set(liveFeedTotalPaxSource,
        apiFeedWithSplitsTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual liveFeedTotalPaxSource
    }

    "Give ApiFeedSource With ApiSplitsWithHistoricalEGateAndFTPercentages as bestPcpPaxEstimate, " +
      "When ApiFeedSource without splitSource and ApiFeedSource with ApiSplitsWithHistoricalEGateAndFTPercentages is present in total pax" in {
      val arrival = arrivalBase.copy(TotalPax = Set(apiFeedWithOutSplitsTotalPaxSource,
        apiFeedWithSplitsTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual apiFeedWithSplitsTotalPaxSource
    }

    "Give ApiFeedSource with Historical as bestPcpPaxEstimate, " +
      "When ApiFeedSource without splitSource and ApiFeedSource with Historical is present in total pax" in {
      val arrival = arrivalBase.copy(TotalPax = Set(apiFeedWithOutSplitsTotalPaxSource,
        apiFeedWithHistoricalSplitsTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual apiFeedWithHistoricalSplitsTotalPaxSource
    }

    "Give Forecast feed source as bestPcpPaxEstimate, " +
      "When Forecast feed source and ApiFeedSource with Historical is present in total pax" in {
      val arrival = arrivalBase.copy(TotalPax = Set(apiFeedWithHistoricalSplitsTotalPaxSource,
        portForecastFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual portForecastFeedTotalPaxSource
    }

    "Gives Forecast feed source as bestPcpPaxEstimate, " +
      "When Forecast feed source and Acl Feed source is present in total pax set" in {
      val arrival = arrivalBase.copy(TotalPax = Set(aclFeedTotalPaxSource,
        portForecastFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual portForecastFeedTotalPaxSource
    }

    "When totalPax " +
      " contain all feed source" +
      " bestPcpPaxEstimate gives Live feed source" in {
      val arrival = arrivalBase.copy(TotalPax = Set(
        liveFeedTotalPaxSource,
        portForecastFeedTotalPaxSource,
        apiFeedWithSplitsTotalPaxSource,
        apiFeedWithHistoricalSplitsTotalPaxSource,
        apiFeedWithOutSplitsTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual liveFeedTotalPaxSource
    }

    "When totalPax" +
      " does not contain Live feed source" +
      " bestPcpPaxEstimate gives apiFeed With ApiSplitsWithHistoricalEGateAndFTPercentages" in {
      val arrival = arrivalBase.copy(TotalPax = Set(
        portForecastFeedTotalPaxSource,
        apiFeedWithSplitsTotalPaxSource,
        apiFeedWithHistoricalSplitsTotalPaxSource,
        apiFeedWithOutSplitsTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual apiFeedWithSplitsTotalPaxSource
    }

    "When totalPax " +
      " does not contain Live feed source and ApiFeed Source with ApiSplitsWithHistoricalEGateAndFTPercentages" +
      " bestPcpPaxEstimate gives port forecast feed" in {
      val arrival = arrivalBase.copy(TotalPax = Set(
        portForecastFeedTotalPaxSource,
        apiFeedWithHistoricalSplitsTotalPaxSource,
        apiFeedWithOutSplitsTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual portForecastFeedTotalPaxSource
    }

    "When totalPax" +
      " does not contain Live feed source, ApiFeed Source with ApiSplitsWithHistoricalEGateAndFTPercentages and port forecast feed source" +
      " bestPcpPaxEstimate gives api with historic feed" in {
      val arrival = arrivalBase.copy(TotalPax = Set(
        apiFeedWithHistoricalSplitsTotalPaxSource,
        apiFeedWithOutSplitsTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual apiFeedWithHistoricalSplitsTotalPaxSource
    }

    "When totalPax" +
      " does not contain Live feed source, ApiFeed Source with ApiSplitsWithHistoricalEGateAndFTPercentages, ApiFeed Source with Historical and port forecast feed source" +
      " bestPcpPaxEstimate gives apiFeed without splitsSource feed" in {
      val arrival = arrivalBase.copy(TotalPax = Set(
        apiFeedWithOutSplitsTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual apiFeedWithOutSplitsTotalPaxSource
    }

    "When totalPax" +
      " does not contain Live feed source, ApiFeed Source with ApiSplitsWithHistoricalEGateAndFTPercentages ," +
      " ApiFeed Source with Historical, port forecast feed source and ApiFeed Source without splits" +
      " bestPcpPaxEstimate gives aclFeed " in {
      val arrival = arrivalBase.copy(TotalPax = Set(aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual aclFeedTotalPaxSource
    }

  }
}
