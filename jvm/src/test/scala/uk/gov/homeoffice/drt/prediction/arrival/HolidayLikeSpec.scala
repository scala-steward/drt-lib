package uk.gov.homeoffice.drt.prediction.arrival

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.{Arrival, ArrivalGeneratorShared}
import uk.gov.homeoffice.drt.prediction.arrival.features.{FeatureColumnsV1, FeatureColumnsV2, OneToManyFeature}
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

case class TestHolidayV1()
                        (implicit
                       val sDateTs: Long => SDateLike,
                       val sDateLocalDate: LocalDate => SDateLike,
                      ) extends OneToManyFeature[Arrival] with FeatureColumnsV1.HolidayLike {
  override val label: String = "testHoliday"
  override val prefix: String = "th"
  override val hols: Seq[(LocalDate, LocalDate)] = Seq(
    (LocalDate(2023, 1, 1), LocalDate(2023, 1, 7)),
    (LocalDate(2024, 1, 1), LocalDate(2024, 1, 10)),
  )
}

case class TestHolidayV2()
                        (implicit
                       val sDateTs: Long => SDateLike,
                       val sDateLocalDate: LocalDate => SDateLike,
                      ) extends OneToManyFeature[Arrival] with FeatureColumnsV2.HolidayLike {
  override val label: String = "testHoliday"
  override val prefix: String = "th"
  override val hols: Seq[(LocalDate, LocalDate)] = Seq(
    (LocalDate(2023, 1, 1), LocalDate(2023, 1, 7)),
    (LocalDate(2024, 1, 1), LocalDate(2024, 1, 10)),
  )
}

class HolidayLikeSpec extends AnyWordSpec with Matchers {
  implicit val sdateTs: Long => SDateLike = ts => SDate(ts)
  implicit val sdateLocal: LocalDate => SDateLike = local => SDate(local)

  "TestHoliday (from V1 holidaylike)" should {
    val holiday = TestHolidayV1()
    "Give consecutive numbers for each day falling within the shortest holiday period (2023)" in {
      val values = (0 until 7).map { d =>
        val arrival = ArrivalGeneratorShared.arrival(sch = SDate("2023-01-1T00:00").addDays(d).millisSinceEpoch)
        holiday.value(arrival)
      }
      values should ===((1 to 7).map(n => Some(n.toString)))
    }
    "Give increasing numbers up to 2023's max for each day falling within the holiday period 2024" in {
      val values = (0 until 10).map { d =>
        val arrival = ArrivalGeneratorShared.arrival(sch = SDate("2024-01-1T00:00").addDays(d).millisSinceEpoch)
        holiday.value(arrival)
      }
      values should ===(Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).map(n => Some(n.toString)))
    }
    "Give 'no' for each day falling outside the holiday period" in {
      val arrivalBeforeHoliday = ArrivalGeneratorShared.arrival(sch = SDate("2023-01-1T00:00").addDays(-1).millisSinceEpoch)
      val arrivalAfterHoliday = ArrivalGeneratorShared.arrival(sch = SDate("2023-01-1T00:00").addDays(7).millisSinceEpoch)
      holiday.value(arrivalBeforeHoliday) should ===(Option("no"))
      holiday.value(arrivalAfterHoliday) should ===(Option("no"))
    }
  }

  "TestHoliday (from V2 holidaylike)" should {
    val holiday = TestHolidayV2()
    "Give consecutive numbers for each day falling within the shortest holiday period (2023)" in {
      val values = (0 until 7).map { d =>
        val arrival = ArrivalGeneratorShared.arrival(sch = SDate("2023-01-1T00:00").addDays(d).millisSinceEpoch)
        holiday.value(arrival)
      }
      values should ===((0 until 7).map(n => Some(n.toString)))
    }
    "Give increasing numbers up to 2023's max for each day falling within the holiday period 2024" in {
      val values = (0 until 10).map { d =>
        val arrival = ArrivalGeneratorShared.arrival(sch = SDate("2024-01-1T00:00").addDays(d).millisSinceEpoch)
        holiday.value(arrival)
      }
      values should ===(Seq(0, 0, 1, 2, 2, 3, 4, 5, 5, 6).map(n => Some(n.toString)))
    }
    "Have identical sets of numbers for each day of each holiday for 2023 vs 2024" in {
      val values2023 = (0 until 7).map { d =>
        val arrival = ArrivalGeneratorShared.arrival(sch = SDate("2023-01-1T00:00").addDays(d).millisSinceEpoch)
        holiday.value(arrival)
      }.toSet
      val values2024 = (0 until 10).map { d =>
        val arrival = ArrivalGeneratorShared.arrival(sch = SDate("2024-01-1T00:00").addDays(d).millisSinceEpoch)
        holiday.value(arrival)
      }.toSet
      values2023 should ===(values2024)
    }
    "Give 'no' for each day falling outside the holiday period" in {
      val arrivalBeforeHoliday = ArrivalGeneratorShared.arrival(sch = SDate("2023-01-1T00:00").addDays(-1).millisSinceEpoch)
      val arrivalAfterHoliday = ArrivalGeneratorShared.arrival(sch = SDate("2023-01-1T00:00").addDays(7).millisSinceEpoch)
      holiday.value(arrivalBeforeHoliday) should ===(Option("no"))
      holiday.value(arrivalAfterHoliday) should ===(Option("no"))
    }
  }
}
