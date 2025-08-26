package uk.gov.homeoffice.drt.prediction.arrival.features

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.prediction.arrival.features.FeatureColumnsV2._
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

class HolidayLikeTest extends AnyWordSpec with Matchers {
  "hasMissingDate" should {
    implicit val now: () => SDateLike = () => SDate("2025-08-26")
    implicit val sDateLocalDate: LocalDate => SDateLike = ld => SDate(ld)
    implicit val sDateTs: Long => SDateLike = ts => SDate(ts)

    "return true if there are no dates" in {
      val holiday = holidayLike(Seq.empty)
      holiday.hasMissingDate shouldBe true
    }

    "return true if the latest date is more than 6 months ago" in {
      val holiday: HolidayLike = holidayLike(Seq(
        (LocalDate(2024, 1, 1), LocalDate(2023, 1, 2)),
        (LocalDate(2025, 2, 18), LocalDate(2025, 2, 20)),
      ))

      holiday.hasMissingDate shouldBe true
    }

    "return false if the latest date is within 6 month ago" in {
      val holiday = holidayLike(Seq(
        (LocalDate(2024, 1, 1), LocalDate(2023, 1, 2)),
        (LocalDate(2025, 2, 28), LocalDate(2025, 2, 30)),
      ))
      holiday.hasMissingDate shouldBe false
    }

    "return false if the latest date is within 6 months in the future" in {
      val holiday = holidayLike(Seq(
        (LocalDate(2024, 1, 1), LocalDate(2023, 1, 2)),
        (LocalDate(2026, 2, 18), LocalDate(2026, 2, 18)),
      ))
      holiday.hasMissingDate shouldBe false
    }

    "return false for today's date plus a month, ie there are no imminently missing dates" in {
      Seq(
        Term1a(),
        OctoberHalfTerm(),
        Term1b(),
        ChristmasHoliday(),
        Term2a(),
        SpringHalfTerm(),
        Term2b(),
        EasterHoliday(),
        Term3a(),
        SummerHalfTerm(),
        Term3b(),
        SummerHoliday(),
        SummerHolidayScotland(),
      )
        .foreach { holiday =>
          val nowInOneMonth: () => SDateLike = () => SDate.now().addMonths(1)
          holiday.hasMissingDate()(nowInOneMonth) shouldBe false
        }
    }
  }

  private def holidayLike(setDates: Seq[(LocalDate, LocalDate)]): HolidayLike =
    new HolidayLike {
      override val dates: Seq[(LocalDate, LocalDate)] = setDates
      override val sDateTs: Long => SDateLike = ts => SDate(ts)
      override implicit val sDateLocalDate: LocalDate => SDateLike = ld => SDate(ld)
    }
}
