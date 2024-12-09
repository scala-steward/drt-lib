package uk.gov.homeoffice.drt.prediction.arrival

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.{Arrival, ArrivalGeneratorShared}
import uk.gov.homeoffice.drt.prediction.arrival.features.FeatureColumnsV1.{DayOfWeek, OneToMany, PartOfDay}
import uk.gov.homeoffice.drt.prediction.arrival.features.OneToManyFeature
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

class ArrivalFeatureValuesExtractorSpec extends AnyWordSpec {
  implicit val sdateProvider: Long => SDateLike = (ts: Long) => SDate(ts)
  val features: Seq[OneToManyFeature[Arrival]] = Seq(DayOfWeek(), PartOfDay())

  val scheduled: SDateLike = SDate("2023-01-01T00:00")
  val scheduledDt: Long = scheduled.millisSinceEpoch
  val scheduledPlus10: Long = scheduled.addMinutes(10).millisSinceEpoch
  val scheduledPlus15: Long = scheduled.addMinutes(15).millisSinceEpoch

  "minutesOffSchedule" should {
    "give the different between scheduled and touchdown, with the day of the week and morning or afternoon flag" in {
      val arrival = ArrivalGeneratorShared.arrival(sch = scheduledDt, act = scheduledPlus10)
      val result = ArrivalFeatureValuesExtractor.minutesOffSchedule(features)(arrival)
      assert(result == Option((10d, Seq("dow_7", "pod_0"), Seq(), arrival.unique.stringValue)))
    }
    "give None when there is no touchdown time" in {
      val arrival = ArrivalGeneratorShared.arrival(sch = scheduledDt)
      val result = ArrivalFeatureValuesExtractor.minutesOffSchedule(features)(arrival)
      assert(result.isEmpty)
    }
  }
  "minutesToChox" should {

    "give the different between chox and touchdown, with the day of the week and morning or afternoon flag" in {
      val arrival = ArrivalGeneratorShared.arrival(sch = scheduledDt, act = scheduledDt, actChox = scheduledPlus15)
      val result = ArrivalFeatureValuesExtractor.minutesToChox(features)(arrival)
      assert(result == Option((15d, Seq("dow_7", "pod_0"), Seq(), arrival.unique.stringValue)))
    }
    "give None when there is no touchdown time" in {
      val arrival = ArrivalGeneratorShared.arrival(sch = scheduledDt, actChox = scheduledPlus15)
      val result = ArrivalFeatureValuesExtractor.minutesToChox(features)(arrival)
      assert(result.isEmpty)
    }
    "give None when there is no actualChox time" in {
      val arrival = ArrivalGeneratorShared.arrival(sch = scheduledDt, act = scheduledPlus15)
      val result = ArrivalFeatureValuesExtractor.minutesToChox(features)(arrival)
      assert(result.isEmpty)
    }
  }
}
