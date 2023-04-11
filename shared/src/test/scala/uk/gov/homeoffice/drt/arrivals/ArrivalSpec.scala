package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.ports._

class ArrivalSpec extends Specification {
  "An Arrival" should {
    "Know it has no source of passengers when there are no sources" in {
      ArrivalGenerator.arrival(totalPax = Map()).hasNoPaxSource shouldEqual(true)
    }
    "Know it has no source of passengers when there are no sources with a pax figure" in {
      ArrivalGenerator.arrival(totalPax = Map(LiveFeedSource -> None)).hasNoPaxSource shouldEqual(true)
    }
    "Know it has a source of passengers when there is a source with a pax figure" in {
      ArrivalGenerator.arrival(totalPax = Map(LiveFeedSource -> Option(100))).hasNoPaxSource shouldEqual(false)
    }
  }

  "Arrival bestPcpPaxEstimate" should {
    val arrivalBase = ArrivalGenerator.arrival()
    val liveFeedTotalPaxSource = (LiveFeedSource -> Option(10))
    val portForecastFeedTotalPaxSource = (ForecastFeedSource -> Option(10))
    val apiFeedTotalPaxSource = (ApiFeedSource -> Option(10))
    val historicApiFeedTotalPaxSource = (HistoricApiFeedSource -> Option(10))
    val aclFeedTotalPaxSource = (AclFeedSource -> Option(10))

    "Give LiveFeedSource as bestPcpPaxEstimate, When LiveFeedSource and ApiFeedSource is present in total pax" in {
      val arrival = arrivalBase.copy(TotalPax = Map(liveFeedTotalPaxSource, apiFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual TotalPaxSource(liveFeedTotalPaxSource._2, liveFeedTotalPaxSource._1)
    }

    "Give Forecast feed source as bestPcpPaxEstimate, " +
      "When Forecast feed source and HistoricApiFeedSource is present in total pax" in {
      val arrival = arrivalBase.copy(TotalPax = Map(historicApiFeedTotalPaxSource, portForecastFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual TotalPaxSource(portForecastFeedTotalPaxSource._2, portForecastFeedTotalPaxSource._1)
    }

    "Give Forecast feed source as bestPcpPaxEstimate, " +
      "When Forecast feed source and Acl Feed source is present in total pax set" in {
      val arrival = arrivalBase.copy(TotalPax = Map(aclFeedTotalPaxSource, portForecastFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual TotalPaxSource(portForecastFeedTotalPaxSource._2, portForecastFeedTotalPaxSource._1)
    }

    "When totalPax " +
      " contain all feed source" +
      " bestPcpPaxEstimate gives Live feed source" in {
      val arrival = arrivalBase.copy(TotalPax = Map(
        liveFeedTotalPaxSource,
        portForecastFeedTotalPaxSource,
        apiFeedTotalPaxSource,
        historicApiFeedTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual TotalPaxSource(liveFeedTotalPaxSource._2, liveFeedTotalPaxSource._1)
    }

    "When totalPax" +
      " does not contain Live feed source" +
      " bestPcpPaxEstimate gives apiFeedSource" in {
      val arrival = arrivalBase.copy(TotalPax = Map(
        portForecastFeedTotalPaxSource,
        apiFeedTotalPaxSource,
        historicApiFeedTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual TotalPaxSource(apiFeedTotalPaxSource._2, apiFeedTotalPaxSource._1)
    }

    "When totalPax " +
      " does not contain Live feed source and ApiFeedSource" +
      " bestPcpPaxEstimate gives port forecast feed" in {
      val arrival = arrivalBase.copy(TotalPax = Map(
        portForecastFeedTotalPaxSource,
        historicApiFeedTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual TotalPaxSource(portForecastFeedTotalPaxSource._2, portForecastFeedTotalPaxSource._1)
    }

    "When totalPax" +
      " does not contain Live feed source, ApiFeedSource and port forecast feed source" +
      " bestPcpPaxEstimate gives api with historic feed" in {
      val arrival = arrivalBase.copy(TotalPax = Map(
        historicApiFeedTotalPaxSource,
        aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual TotalPaxSource(historicApiFeedTotalPaxSource._2, historicApiFeedTotalPaxSource._1)
    }

    "When totalPax" +
      " does not contain Live feed source, ApiFeedSource ," +
      " HistoricApiFeedSource , port forecast feed source and ApiFeed Source without splits" +
      " bestPcpPaxEstimate gives aclFeed " in {
      val arrival = arrivalBase.copy(TotalPax = Map(aclFeedTotalPaxSource))
      arrival.bestPcpPaxEstimate mustEqual TotalPaxSource(aclFeedTotalPaxSource._2, aclFeedTotalPaxSource._1)
    }

    "When totalPax" +
      " for a LiveFeedSource is less than Transfer passenger numbers and AclFeedSource is more than Transfer passenger number," +
      " then bestPcpPaxEstimate gives LiveFeedSource with zero pax " in {
      val arrival = arrivalBase.copy(TranPax = Option(100),
        TotalPax = Map(AclFeedSource -> Option(250), LiveFeedSource -> Option(50)))
      arrival.bestPcpPaxEstimate mustEqual TotalPaxSource(Option(0), LiveFeedSource)
    }

    "When totalPax" +
      " does not contain any SourceData," +
      " then bestPcpPaxEstimate fallback to FeedSource " in {
      val arrival = arrivalBase.copy(ActPax = Option(10), TotalPax = Map(), FeedSources = Set(AclFeedSource))
      arrival.bestPcpPaxEstimate mustEqual TotalPaxSource(aclFeedTotalPaxSource._2, aclFeedTotalPaxSource._1)
    }
  }
  "fallBackToFeedSource should return known pax when the arrival feed sources contain one of live, forecast or acl" >> {
    "when there is no act or trans we should get TotalPaxSource(None, _)" >> {
      val arrival = ArrivalGenerator.arrival(feedSources = Set(LiveFeedSource), actPax = None, tranPax = None)

      arrival.fallBackToFeedSource === Option(TotalPaxSource(None, LiveFeedSource))
    }
    "when there is no act but some trans we should get TotalPaxSource(None, _)" >> {
      val arrival = ArrivalGenerator.arrival(feedSources = Set(LiveFeedSource), actPax = None, tranPax = Option(100))

      arrival.fallBackToFeedSource === Option(TotalPaxSource(None, LiveFeedSource))
    }
    "when there is some act but no trans we should get TotalPaxSource(Some(act), _)" >> {
      val arrival = ArrivalGenerator.arrival(feedSources = Set(LiveFeedSource), actPax = Option(100), tranPax = None)

      arrival.fallBackToFeedSource === Option(TotalPaxSource(Option(100), LiveFeedSource))
    }
    "when there is some act and trans we should get TotalPaxSource(Some(act - trans), _)" >> {
      val arrival = ArrivalGenerator.arrival(feedSources = Set(LiveFeedSource), actPax = Option(100), tranPax = Option(25))

      arrival.fallBackToFeedSource === Option(TotalPaxSource(Option(75), LiveFeedSource))
    }
    "when there is some act and trans where trans > act we should get TotalPaxSource(Some(0), _)" >> {
      val arrival = ArrivalGenerator.arrival(feedSources = Set(LiveFeedSource), actPax = Option(100), tranPax = Option(125))

      arrival.fallBackToFeedSource === Option(TotalPaxSource(Option(0), LiveFeedSource))
    }
  }
}
