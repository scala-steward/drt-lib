package uk.gov.homeoffice.drt.prediction.arrival

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.{Arrival, ArrivalGenerator}
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{HolidayLike, OneToMany}
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

case class TestHoliday()
                      (implicit
                       val sDateTs: Long => SDateLike,
                       val sDateLocalDate: LocalDate => SDateLike,
                      ) extends OneToMany[Arrival] with HolidayLike {
  override val label: String = "testHoliday"
  override val prefix: String = "th"
  override val hols: Seq[(LocalDate, LocalDate)] = Seq(
    (LocalDate(2023, 1, 1), LocalDate(2023, 1, 7)),
  )
}

class HolidayLikeSpec extends AnyWordSpec with Matchers {
  implicit val sdateTs: Long => SDateLike = ts => SDate(ts)
  implicit val sdateLocal: LocalDate => SDateLike = local => SDate(local)

  "TestHoliday" should {
    val holiday = TestHoliday()
    "Give sequential numbers for each day falling within the holiday period" in {
      val values = (0 until 7).map { d =>
        val arrival = ArrivalGenerator.arrival(sch = SDate("2023-01-1T00:00").addDays(d).millisSinceEpoch)
        holiday.value(arrival)
      }
      values should ===((1 to 7).map(n => Some(n.toString)))
    }
    "Give 'no' for each day falling outside the holiday period" in {
      val arrivalBeforeHoliday = ArrivalGenerator.arrival(sch = SDate("2023-01-1T00:00").addDays(-1).millisSinceEpoch)
      val arrivalAfterHoliday = ArrivalGenerator.arrival(sch = SDate("2023-01-1T00:00").addDays(7).millisSinceEpoch)
      holiday.value(arrivalBeforeHoliday) should ===(Option("no"))
      holiday.value(arrivalAfterHoliday) should ===(Option("no"))
    }
  }
}
