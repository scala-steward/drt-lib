package uk.gov.homeoffice.drt.protobuf.serialisation

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.arrivals._
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage.{FlightMessage, PassengersMessage, TotalPaxSourceMessage}

class FlightMessagePassengersConversionSpec extends Specification {
  "V1 messages - no feed or pax sources" >> {
    "No act chox" >> {
      "old pax should be assigned to UnknownFeedSource" >> {
        val v1Message = FlightMessage(
          actPaxOLD = Option(95),
          tranPaxOLD = Option(10),
        )
        val arrival = FlightMessageConversion.flightMessageToApiFlight(v1Message)
        arrival.PassengerSources === Map(
          UnknownFeedSource -> Passengers(Option(95), Option(10)),
        )
      }
    }
    "With act chox" >> {
      "old pax should be assigned to LiveFeedSource" >> {
        val v1Message = FlightMessage(
          actPaxOLD = Option(95),
          tranPaxOLD = Option(10),
          actualChox = Option(1234L)
        )
        val arrival = FlightMessageConversion.flightMessageToApiFlight(v1Message)
        arrival.PassengerSources === Map(
          LiveFeedSource -> Passengers(Option(95), Option(10)),
        )
      }
    }
    "With act chox & api" >> {
      "old pax should be assigned to LiveFeedSource, and api to ApiFeedSource using live tran pax" >> {
        val v1Message = FlightMessage(
          actPaxOLD = Option(95),
          tranPaxOLD = Option(10),
          apiPaxOLD = Option(101),
          actualChox = Option(1234L)
        )
        val arrival = FlightMessageConversion.flightMessageToApiFlight(v1Message)
        arrival.PassengerSources === Map(
          ApiFeedSource -> Passengers(Option(101), Option(10)),
          LiveFeedSource -> Passengers(Option(95), Option(10)),
        )
      }
    }
  }

  "V2 messages - feed sources but no pax sources" >> {
    "ForecastFeedSource & AclFeedSource" >> {
      "old pax should be assigned to the best source, ie ForecastFeedSource" >> {
        val v1Message = FlightMessage(
          actPaxOLD = Option(95),
          tranPaxOLD = Option(10),
          feedSources = Seq(ForecastFeedSource.toString, AclFeedSource.toString),
        )
        val arrival = FlightMessageConversion.flightMessageToApiFlight(v1Message)
        arrival.PassengerSources === Map(
          ForecastFeedSource -> Passengers(Option(95), Option(10)),
          AclFeedSource -> Passengers(None, None),
        )
      }
    }
    "ForecastFeedSource & LiveFeedSource, and api pax" >> {
      "old pax should be assigned to the best source, ie LiveFeedSource, and api to api with trans from LiveFeedSource" >> {
        val v1Message = FlightMessage(
          actPaxOLD = Option(95),
          tranPaxOLD = Option(10),
          feedSources = Seq(LiveFeedSource.toString, ForecastFeedSource.toString),
          apiPaxOLD = Option(101),
        )
        val arrival = FlightMessageConversion.flightMessageToApiFlight(v1Message)
        arrival.PassengerSources === Map(
          LiveFeedSource -> Passengers(Option(95), Option(10)),
          ForecastFeedSource -> Passengers(None, None),
          ApiFeedSource -> Passengers(Option(101), Option(10)),
        )
      }
    }
  }

  "V3 messages - pax sources without tran pax" >> {
    "ForecastFeedSource & AclFeedSource" >> {
      "old tran pax should be assigned to the best source, ie ForecastFeedSource" >> {
        val v1Message = FlightMessage(
          actPaxOLD = Option(50),
          tranPaxOLD = Option(10),
          totalPax = Seq(
            TotalPaxSourceMessage(Option(ForecastFeedSource.toString), None, Option(95)),
            TotalPaxSourceMessage(Option(AclFeedSource.toString), None, Option(120)),
          ),
        )
        val arrival = FlightMessageConversion.flightMessageToApiFlight(v1Message)
        arrival.PassengerSources === Map(
          ForecastFeedSource -> Passengers(Option(95), Option(10)),
          AclFeedSource -> Passengers(Option(120), None),
        )
      }
    }
    "ForecastFeedSource & AclFeedSource, and api pax" >> {
      "old tran pax should be assigned to the best source, ie ForecastFeedSource, and api to api with trans from ForecastFeedSource" >> {
        val v1Message = FlightMessage(
          actPaxOLD = Option(50),
          tranPaxOLD = Option(10),
          totalPax = Seq(
            TotalPaxSourceMessage(Option(ForecastFeedSource.toString), None, Option(95)),
            TotalPaxSourceMessage(Option(AclFeedSource.toString), None, Option(120)),
          ),
          apiPaxOLD = Option(101),
        )
        val arrival = FlightMessageConversion.flightMessageToApiFlight(v1Message)
        arrival.PassengerSources === Map(
          ForecastFeedSource -> Passengers(Option(95), Option(10)),
          AclFeedSource -> Passengers(Option(120), None),
          ApiFeedSource -> Passengers(Option(101), Option(10)),
        )
      }
    }
  }

  "V4 messages - pax sources with tran pax" >> {
    "ForecastFeedSource & AclFeedSource" >> {
      val v1Message = FlightMessage(
        actPaxOLD = Option(50),
        tranPaxOLD = Option(5),
        totalPax = Seq(
          TotalPaxSourceMessage(Option(ForecastFeedSource.toString), Option(PassengersMessage(Option(100), Option(10))), None),
          TotalPaxSourceMessage(Option(AclFeedSource.toString), Option(PassengersMessage(Option(120), None)), None),
          TotalPaxSourceMessage(Option(ApiFeedSource.toString), Option(PassengersMessage(Option(99), Option(11))), None),
        ),
      )
      val arrival = FlightMessageConversion.flightMessageToApiFlight(v1Message)
      arrival.PassengerSources === Map(
        ForecastFeedSource -> Passengers(Option(100), Option(10)),
        AclFeedSource -> Passengers(Option(120), None),
        ApiFeedSource -> Passengers(Option(99), Option(11)),
      )
    }
  }
}
