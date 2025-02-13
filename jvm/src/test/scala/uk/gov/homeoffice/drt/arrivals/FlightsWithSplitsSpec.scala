package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.ports.{AclFeedSource, ApiFeedSource, ForecastFeedSource, HistoricApiFeedSource, LiveFeedSource, MlFeedSource, ScenarioSimulationSource}
import uk.gov.homeoffice.drt.time.SDate

class FlightsWithSplitsSpec extends Specification{
  "When filtering flights by Scheduled date" >> {
    val sourceOrderPreference = List(
      ScenarioSimulationSource,
      LiveFeedSource,
      ApiFeedSource,
      ForecastFeedSource,
      MlFeedSource,
      HistoricApiFeedSource,
      AclFeedSource,
    )
    "Given a flight with Splits containing flights inside and outside the range" >> {
      "Then I should only get flights scheduled inside the range" >> {
        val fws1 = ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(SDate("2020-09-22T10:00"))
        val fws2 = ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(SDate("2020-09-21T10:00"))
        val fws3 = ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(SDate("2020-09-23T11:00"))
        val flightsWithSplits = FlightsWithSplits(Map(
          fws1.unique -> fws1,
          fws2.unique -> fws2,
          fws3.unique -> fws3
        ))

        val start = SDate("2020-09-21T10:00").getUtcLastMidnight
        val end = start.addDays(1)
        val result = flightsWithSplits.scheduledOrPcpWindow(start, end, sourceOrderPreference)

        val expected = FlightsWithSplits(Map(fws2.unique -> fws2))

        result === expected
      }
    }
  }
}
