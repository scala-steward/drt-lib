package uk.gov.homeoffice.drt.prediction.arrival

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.{Arrival, ArrivalGenerator}
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{DayOfWeek, OneToManyFeatureColumn}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

class OneToManyFeatureColumnsTest extends AnyWordSpec with Matchers {
  implicit val sdate: Long => SDateLike = (ts: Long) => SDate(ts)
  "something" should {
    "do something" in {
      val c = OneToManyFeatureColumn.fromLabel(DayOfWeek.label)

      val thursdayMarch092023 = SDate("2023-03-09T00:00").millisSinceEpoch
      val thursday = "4"

      val arrival = ArrivalGenerator.arrival(sch = thursdayMarch092023)

      val x = c match {
        case other: OneToManyFeatureColumn[Arrival] => other.value(arrival)
      }

      x should ===(thursday)
    }
  }
}
