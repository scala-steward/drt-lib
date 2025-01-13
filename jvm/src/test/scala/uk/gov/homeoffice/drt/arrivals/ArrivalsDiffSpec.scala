package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.ports.{AclFeedSource, PortCode}

class ArrivalsDiffSpec extends Specification {
  val now: Long = 10L

  "When I apply ArrivalsDiff to FlightsWithSplits" >> {
    val arrival = ArrivalGenerator.arrival(iata = "BA0001", status = ArrivalStatus("new status"), feedSource = AclFeedSource)
    val existing = FlightsWithSplits(Map(arrival.unique -> ApiFlightWithSplits(arrival, Set())))

    val updatedArrival = arrival.copy(PreviousPort = Option(PortCode("CDG")))
    val arrivalsDiff = ArrivalsDiff(Seq(updatedArrival), Seq())

    arrivalsDiff.applyTo(existing, now, List(AclFeedSource))._1 === FlightsWithSplits(Map(arrival.unique -> ApiFlightWithSplits(updatedArrival, Set(), Option(10L))))
  }

  "When diff existing arrivals with the updates" >> {
    "Given no new arrivals and" >> {
      val arrivalsDiff = ArrivalsDiff(Seq(), Seq())

      "No existing flights" >> {
        "Then I should get an empty FlightsWithSplits" >> {
          val updated = arrivalsDiff.diff(Map())
          updated === ArrivalsDiff.empty
        }
      }

      "One existing flight" >> {
        val arrival = ArrivalGeneratorShared.arrival(iata = "BA0001")
        val flights = Map(arrival.unique -> arrival)

        "Then I should get an empty FlightsWithSplits" >> {
          val updated = arrivalsDiff.diff(flights)
          updated === ArrivalsDiff.empty
        }
      }
    }

    "Given one new arrival and" >> {
      val arrival = ArrivalGeneratorShared.arrival(iata = "BA0001", status = ArrivalStatus("new status"))
      val arrivalsDiff = ArrivalsDiff(Seq(arrival), Seq())

      "No existing flights" >> {
        "Then I should get a FlightsWithSplits containing the new arrival" >> {
          val updated = arrivalsDiff.diff(Map())
          updated === ArrivalsDiff(Seq(arrival), Seq())
        }
      }

      "One existing flight with splits that matches and has the same arrival" >> {
        val flights = Map(arrival.unique -> arrival)

        "Then I should get an empty FlightsWithSplits" >> {
          val updated = arrivalsDiff.diff(flights)
          updated === ArrivalsDiff.empty
        }
      }

      "One existing flight with splits that matches and has an older arrival" >> {
        val olderArrival = arrival.copy(Status = ArrivalStatus("old status"))
        val flights = Map(olderArrival.unique -> olderArrival)

        "Then I should get a FlightsWithSplits with the updated arrival and existing splits" >> {
          val updated = arrivalsDiff.diff(flights)
          updated === ArrivalsDiff(Seq(arrival), Seq())
        }
      }
    }
  }
}
