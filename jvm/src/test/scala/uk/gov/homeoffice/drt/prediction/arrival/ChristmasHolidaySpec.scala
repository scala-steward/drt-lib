package uk.gov.homeoffice.drt.prediction.arrival

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.ArrivalGenerator
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.ChristmasHoliday
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

class ChristmasHolidaySpec extends AnyWordSpec with Matchers {
  implicit val sdateTs: Long => SDateLike = ts => SDate(ts)
  implicit val sdateLocal: LocalDate => SDateLike = local => SDate(local)

  "ChristmasHoliday" should {
    "Give 'no' for 16/12 falling outside of xmas 2022 (19/12 - 02/01) with a buffer of 2 days" in {
      val arrival = ArrivalGenerator.arrival(sch = SDate("2022-12-16T00:00").millisSinceEpoch)
      val holiday = ChristmasHoliday()
      holiday.value(arrival) should ===(Option("no"))
    }
    "Give '0' for 17/12 falling inside of xmas 2022 (19/12 - 02/01) with a buffer of 2 days" in {
      val arrival = ArrivalGenerator.arrival(sch = SDate("2022-12-17T00:00").millisSinceEpoch)
      val holiday = ChristmasHoliday()
      holiday.value(arrival) should ===(Option("0"))
    }
    "Give '1' for 18/12 falling inside of xmas 2022 (19/12 - 02/01) with a buffer of 2 days" in {
      val arrival = ArrivalGenerator.arrival(sch = SDate("2022-12-18T00:00").millisSinceEpoch)
      val holiday = ChristmasHoliday()
      holiday.value(arrival) should ===(Option("1"))
    }
    "Give '15' for 01/01 falling outside of xmas 2022 (19/12 - 02/01) with a buffer of 2 days" in {
      val arrival = ArrivalGenerator.arrival(sch = SDate("2023-01-01T00:00").millisSinceEpoch)
      val holiday = ChristmasHoliday()
      holiday.value(arrival) should ===(Option("15"))
    }
    "Give '16' for 02/01 falling outside of xmas 2022 (19/12 - 02/01) with a buffer of 2 days" in {
      val arrival = ArrivalGenerator.arrival(sch = SDate("2023-01-02T00:00").millisSinceEpoch)
      val holiday = ChristmasHoliday()
      holiday.value(arrival) should ===(Option("16"))
    }
    "Give '17' for 03/01 falling outside of xmas 2022 (19/12 - 02/01) with a buffer of 2 days" in {
      val arrival = ArrivalGenerator.arrival(sch = SDate("2023-01-03T00:00").millisSinceEpoch)
      val holiday = ChristmasHoliday()
      holiday.value(arrival) should ===(Option("17"))
    }
    "Give '18' for 04/01 falling outside of xmas 2022 (19/12 - 02/01) with a buffer of 2 days" in {
      val arrival = ArrivalGenerator.arrival(sch = SDate("2023-01-04T00:00").millisSinceEpoch)
      val holiday = ChristmasHoliday()
      holiday.value(arrival) should ===(Option("18"))
    }
    "Give 'no' for 05/01 falling outside of xmas 2022 (19/12 - 02/01) with a buffer of 2 days" in {
      val arrival = ArrivalGenerator.arrival(sch = SDate("2023-01-05T00:00").millisSinceEpoch)
      val holiday = ChristmasHoliday()
      holiday.value(arrival) should ===(Option("no"))
    }
  }
}
