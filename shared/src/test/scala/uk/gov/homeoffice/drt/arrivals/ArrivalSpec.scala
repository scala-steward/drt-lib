package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.ports._

class ArrivalSpec extends Specification {
  "Arrival bestPcpPaxEstimate" should {
    val arrivalBase = ArrivalGenerator.arrival()
    val liveFeedTotalPaxSource = TotalPaxSource(Option(10), LiveFeedSource)
    val portForecastFeedTotalPaxSource = TotalPaxSource(Option(10), ForecastFeedSource)
    val apiFeedTotalPaxSource = TotalPaxSource(Option(10), ApiFeedSource)
    val historicApiFeedTotalPaxSource = TotalPaxSource(Option(10), HistoricApiFeedSource)
    val aclFeedTotalPaxSource = TotalPaxSource(Option(10), AclFeedSource)

    "Give LiveFeedSource as bestPcpPaxEstimate, When LiveFeedSource and ApiFeedSource is present in total pax" in {
      val arrival = arrivalBase.copy(TotalPax = Set(liveFeedTotalPaxSource,
        apiFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual liveFeedTotalPaxSource
    }

    "Give Forecast feed source as bestPcpPaxEstimate, " +
      "When Forecast feed source and HistoricApiFeedSource is present in total pax" in {
      val arrival = arrivalBase.copy(TotalPax = Set(historicApiFeedTotalPaxSource,
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
        apiFeedTotalPaxSource,
        historicApiFeedTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual liveFeedTotalPaxSource
    }

    "When totalPax" +
      " does not contain Live feed source" +
      " bestPcpPaxEstimate gives apiFeedSource" in {
      val arrival = arrivalBase.copy(TotalPax = Set(
        portForecastFeedTotalPaxSource,
        apiFeedTotalPaxSource,
        historicApiFeedTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual apiFeedTotalPaxSource
    }

    "When totalPax " +
      " does not contain Live feed source and ApiFeedSource" +
      " bestPcpPaxEstimate gives port forecast feed" in {
      val arrival = arrivalBase.copy(TotalPax = Set(
        portForecastFeedTotalPaxSource,
        historicApiFeedTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual portForecastFeedTotalPaxSource
    }

    "When totalPax" +
      " does not contain Live feed source, ApiFeedSource and port forecast feed source" +
      " bestPcpPaxEstimate gives api with historic feed" in {
      val arrival = arrivalBase.copy(TotalPax = Set(
        historicApiFeedTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual historicApiFeedTotalPaxSource
    }

    "When totalPax" +
      " does not contain Live feed source, ApiFeedSource ," +
      " HistoricApiFeedSource , port forecast feed source and ApiFeed Source without splits" +
      " bestPcpPaxEstimate gives aclFeed " in {
      val arrival = arrivalBase.copy(TotalPax = Set(aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual aclFeedTotalPaxSource
    }

    "When totalPax" +
      " for a LiveFeedSource is less than Transfer passenger numbers and AclFeedSource is more than Transfer passenger number," +
      " then bestPcpPaxEstimate gives LiveFeedSource with zero pax " in {
      val arrival = arrivalBase.copy(TranPax = Option(100),
        TotalPax = Set(aclFeedTotalPaxSource.copy(pax = Option(250)), liveFeedTotalPaxSource.copy(pax = Option(50))))
      arrival.bestPcpPaxEstimate mustEqual liveFeedTotalPaxSource.copy(pax = Option(0))
    }

    "When totalPax" +
      " does not contain any SourceData," +
      " then bestPcpPaxEstimate fallback to FeedSource " in {
      val arrival = arrivalBase.copy(ActPax = Option(10), TotalPax = Set(), FeedSources = Set(AclFeedSource))
      arrival.bestPcpPaxEstimate mustEqual aclFeedTotalPaxSource
    }
  }
}
