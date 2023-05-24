package uk.gov.homeoffice.drt.prediction.arrival

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.{Arrival, ArrivalGenerator}
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{DayOfWeek, OneToMany}
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

class OneToManyColumnsTest extends AnyWordSpec with Matchers {
  implicit val sdateFromLong: Long => SDateLike = (ts: Long) => SDate(ts)
  implicit val sdateFromLocal: LocalDate => SDateLike = (ts: LocalDate) => SDate(ts)
  "DayOfWeek" should {
    "return the correct day index for an arrival's scheduled date" in {
      val c = OneToMany.fromLabel(DayOfWeek.label)

      val thursdayMarch092023 = SDate("2023-03-09T00:00").millisSinceEpoch
      val thursdayIdx = 4

      val arrival = ArrivalGenerator.arrival(sch = thursdayMarch092023)

      val dayOfTheWeekIndex = c match {
        case other: OneToMany[Arrival] => other.value(arrival)
      }

      dayOfTheWeekIndex should ===(Some(thursdayIdx.toString))
    }
  }
}
