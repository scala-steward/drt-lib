package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification

class ArrivalsDiffSpec extends Specification {
  val now: Long = 10L

  "When I apply ArrivalsDiff to FlightsWithSplits" >> {
    "Given no new arrivals and" >> {
      val arrivalsDiff = ArrivalsDiff(Seq(), Seq())

      "No existing flights" >> {
        "Then I should get an empty FlightsWithSplits" >> {
          val updated = arrivalsDiff.diffWith(Map())
          updated === ArrivalsDiff.empty
        }
      }

      "One existing flight" >> {
        val arrival = ArrivalGenerator.arrival(iata = "BA0001")
        val flights = Map(arrival.unique -> arrival)

        "Then I should get an empty FlightsWithSplits" >> {
          val updated = arrivalsDiff.diffWith(flights)
          updated === ArrivalsDiff.empty
        }
      }
    }

    "Given one new arrival and" >> {
      val arrival = ArrivalGenerator.arrival(iata = "BA0001", status = ArrivalStatus("new status"))
      val arrivalsDiff = ArrivalsDiff(Seq(arrival), Seq())

      "No existing flights" >> {
        "Then I should get a FlightsWithSplits containing the new arrival" >> {
          val updated = arrivalsDiff.diffWith(Map())
          updated === ArrivalsDiff(Seq(arrival), Seq())
        }
      }

      "One existing flight with splits that matches and has the same arrival" >> {
        val flights = Map(arrival.unique -> arrival)

        "Then I should get an empty FlightsWithSplits" >> {
          val updated = arrivalsDiff.diffWith(flights)
          updated === ArrivalsDiff.empty
        }
      }

      "One existing flight with splits that matches and has an older arrival" >> {
        val olderArrival = arrival.copy(Status = ArrivalStatus("old status"))
        val flights = Map(olderArrival.unique -> olderArrival)

        "Then I should get a FlightsWithSplits with the updated arrival and existing splits" >> {
          val updated = arrivalsDiff.diffWith(flights)
          updated === ArrivalsDiff(Seq(arrival), Seq())
        }
      }
    }
  }
}
