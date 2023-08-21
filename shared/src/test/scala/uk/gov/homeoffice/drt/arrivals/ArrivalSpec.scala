package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.ports._

class ArrivalSpec extends Specification {


  "An Arrival" should {
    "Know it has no source of passengers when there are no sources" in {
      ArrivalGenerator.arrival(passengerSources = Map()).hasNoPaxSource shouldEqual true
    }
    "Know it has no source of passengers when there are no sources with a pax figure" in {
      ArrivalGenerator.arrival(passengerSources = Map(LiveFeedSource -> Passengers(None, None))).hasNoPaxSource shouldEqual true
    }
    "Know it has a source of passengers when there is a source with a pax figure" in {
      ArrivalGenerator.arrival(passengerSources = Map(LiveFeedSource -> Passengers(Option(100), None))).hasNoPaxSource shouldEqual false
    }
  }

  "Arrival bestPcpPaxEstimate" should {
    val arrivalBase = ArrivalGenerator.arrival()
    val liveFeedPaxSource = LiveFeedSource -> Passengers(Option(10), None)
    val portForecastFeedPaxSource = ForecastFeedSource -> Passengers(Option(10), None)
    val apiFeedPaxSource = ApiFeedSource -> Passengers(Option(10), None)
    val historicApiFeedPaxSource = HistoricApiFeedSource -> Passengers(Option(10), None)
    val aclFeedPaxSource = AclFeedSource -> Passengers(Option(10), None)

    val sourceOrderPreference = List(
      ScenarioSimulationSource,
      LiveFeedSource,
      ApiFeedSource,
      ForecastFeedSource,
      MlFeedSource,
      HistoricApiFeedSource,
      AclFeedSource,
    )

    "Give LiveFeedSource as bestPcpPaxEstimate, When LiveFeedSource and ApiFeedSource is present in total pax" in {
      val arrival = arrivalBase.copy(PassengerSources = Map(liveFeedPaxSource, apiFeedPaxSource))
      arrival.bestPaxEstimate(sourceOrderPreference) mustEqual PaxSource(liveFeedPaxSource._1, liveFeedPaxSource._2)
    }

    "Give Forecast feed source as bestPcpPaxEstimate, " +
      "When Forecast feed source and HistoricApiFeedSource is present in total pax" in {
      val arrival = arrivalBase.copy(PassengerSources = Map(historicApiFeedPaxSource, portForecastFeedPaxSource))
      arrival.bestPaxEstimate(sourceOrderPreference) mustEqual PaxSource(portForecastFeedPaxSource._1, portForecastFeedPaxSource._2)
    }

    "Give Forecast feed source as bestPcpPaxEstimate, " +
      "When Forecast feed source and Acl Feed source is present in total pax set" in {
      val arrival = arrivalBase.copy(PassengerSources = Map(aclFeedPaxSource, portForecastFeedPaxSource))
      arrival.bestPaxEstimate(sourceOrderPreference) mustEqual PaxSource(portForecastFeedPaxSource._1, portForecastFeedPaxSource._2)
    }

    "When totalPax " +
      " contain all feed source" +
      " bestPcpPaxEstimate gives Live feed source" in {
      val arrival = arrivalBase.copy(PassengerSources = Map(
        liveFeedPaxSource,
        portForecastFeedPaxSource,
        apiFeedPaxSource,
        historicApiFeedPaxSource,
        aclFeedPaxSource))
      arrival.bestPaxEstimate(sourceOrderPreference) mustEqual PaxSource(liveFeedPaxSource._1, liveFeedPaxSource._2)
    }

    "When totalPax" +
      " does not contain Live feed source" +
      " bestPcpPaxEstimate gives apiFeedSource" in {
      val arrival = arrivalBase.copy(PassengerSources = Map(
        portForecastFeedPaxSource,
        apiFeedPaxSource,
        historicApiFeedPaxSource,
        aclFeedPaxSource))
      arrival.bestPaxEstimate(sourceOrderPreference) mustEqual PaxSource(apiFeedPaxSource._1, apiFeedPaxSource._2)
    }

    "When totalPax " +
      " does not contain Live feed source and ApiFeedSource" +
      " bestPcpPaxEstimate gives port forecast feed" in {
      val arrival = arrivalBase.copy(PassengerSources = Map(
        portForecastFeedPaxSource,
        historicApiFeedPaxSource,
        aclFeedPaxSource))
      arrival.bestPaxEstimate(sourceOrderPreference) mustEqual PaxSource(portForecastFeedPaxSource._1, portForecastFeedPaxSource._2)
    }

    "When totalPax" +
      " does not contain Live feed source, ApiFeedSource and port forecast feed source" +
      " bestPcpPaxEstimate gives api with historic feed" in {
      val arrival = arrivalBase.copy(PassengerSources = Map(
        historicApiFeedPaxSource,
        aclFeedPaxSource))
      arrival.bestPaxEstimate(sourceOrderPreference) mustEqual PaxSource(historicApiFeedPaxSource._1, historicApiFeedPaxSource._2)
    }

    "When totalPax" +
      " does not contain Live feed source, ApiFeedSource ," +
      " HistoricApiFeedSource , port forecast feed source and ApiFeed Source without splits" +
      " bestPcpPaxEstimate gives aclFeed " in {
      val arrival = arrivalBase.copy(PassengerSources = Map(aclFeedPaxSource))
      arrival.bestPaxEstimate(sourceOrderPreference) mustEqual PaxSource(aclFeedPaxSource._1, aclFeedPaxSource._2)
    }

    "When totalPax" +
      " for a LiveFeedSource is less than Transfer passenger numbers and AclFeedSource is more than Transfer passenger number," +
      " then bestPcpPaxEstimate gives LiveFeedSource with zero pax " in {
      val arrival = arrivalBase.copy(
        PassengerSources = Map(AclFeedSource -> Passengers(Option(250), None), LiveFeedSource -> Passengers(Option(50), Option(100))))
      arrival.bestPaxEstimate(sourceOrderPreference) mustEqual PaxSource(LiveFeedSource, Passengers(Option(50), Option(100)))
      arrival.bestPcpPaxEstimate(sourceOrderPreference) must beSome(0)
    }

    "When totalPax" +
      " does not contain any SourceData," +
      " then bestPcpPaxEstimate fallback to FeedSource " in {
      val arrival = arrivalBase.copy(PassengerSources = Map(AclFeedSource -> Passengers(Option(10), None)), FeedSources = Set(AclFeedSource))
      arrival.bestPaxEstimate(sourceOrderPreference) mustEqual PaxSource(aclFeedPaxSource._1, aclFeedPaxSource._2)
    }
  }

  "isInRange" >> {
    "should return true when the needle is equal to the start of the range" >> {
      Arrival.isInRange(1, 10)(1) === true
    }
    "should return true when the needle is equal to the end of the range" >> {
      Arrival.isInRange(1, 10)(10) === true
    }
    "should return true when the needle is between the start and end of the range" >> {
      Arrival.isInRange(1, 10)(5) === true
    }
    "should return false when the needle is lower than the start of the range" >> {
      Arrival.isInRange(1, 10)(0) === false
    }
    "should return false when the needle is higher than the end of the range" >> {
      Arrival.isInRange(1, 10)(11) === false
    }
  }
}
