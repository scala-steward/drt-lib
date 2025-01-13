package uk.gov.homeoffice.drt.arrivals

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2, Terminal}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

class FlightsWithSplitsDiffSpec extends Specification {

  val arrival: Arrival = ArrivalGeneratorShared.arrival()

  val arrivalWithSplits: ApiFlightWithSplits = ApiFlightWithSplits(arrival, Set(), None)

  def arrivalForDate(date: SDateLike): Arrival = ArrivalGeneratorShared.arrival(sch = date.millisSinceEpoch)

  def arrivalForDateAndTerminal(date: SDateLike, terminal: Terminal): Arrival =
    ArrivalGeneratorShared.arrival(sch = date.millisSinceEpoch, terminal = terminal)

  "Given a FlightsWithSplitsDiff with no updates and no removals then isEmpty should be true" >> {
    val diff = FlightsWithSplitsDiff(List())
    val result = diff.isEmpty

    result === true
  }

  "Given a FlightsWithSplitsDiff with one update and no removals then isEmpty should be false" >> {
    val diff = FlightsWithSplitsDiff(List(arrivalWithSplits))
    val result = diff.isEmpty

    result === false
  }

  "Given a FlightsWithSplitsDiff with one update and one removal on the filter day then I should get both" >> {
    val date = SDate("2020-09-21")
    val diff = FlightsWithSplitsDiff(List(ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(date)))

    val result = diff.window(date.millisSinceEpoch, date.addDays(1).millisSinceEpoch)

    result === diff
  }

  "Given a FlightsWithSplitsDiff with one update on the filter and one before I should just get back the one on the day" >> {
    val filterDate = SDate("2020-09-21")
    val otherDate = SDate("2020-09-20")
    val diff = FlightsWithSplitsDiff(
      List(
        ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(filterDate),
        ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(otherDate)
      )
    )
    val expected = FlightsWithSplitsDiff(
      List(ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(filterDate))
    )

    val result = diff.window(filterDate.millisSinceEpoch, filterDate.addDays(1).millisSinceEpoch)

    result === expected
  }

  "Given a FlightsWithSplitsDiff updates and removals before, on and after the filter date " +
    "Then I should only get back arrivals on the filter date" >> {
    val filterDate = SDate("2020-09-21")
    val beforeDate = SDate("2020-09-20")
    val afterDate = SDate("2020-09-22")

    val diff = FlightsWithSplitsDiff(
      List(
        ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(filterDate),
        ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(beforeDate),
        ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(afterDate)
      )
    )
    val expected = FlightsWithSplitsDiff(
      List(ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(filterDate))
    )

    val result = diff.window(
      filterDate.millisSinceEpoch,
      filterDate.addDays(1).addMillis(-1).millisSinceEpoch
    )

    result === expected
  }

  "Given a FlightsWithSplitsDiff updates and removals on two terminals and I filter by terminal " +
    "Then I should only see the flights for the filter terminal" >> {
    val date = SDate("2020-09-21")

    val diff = FlightsWithSplitsDiff(
      List(
        ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(date, T1),
        ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(date, T2)
      )
    )

    val expected = FlightsWithSplitsDiff(
      List(
        ArrivalGeneratorShared.flightWithSplitsForDayAndTerminal(date, T1)
      )
    )

    val result = diff.forTerminal(T1)

    result === expected
  }
}
