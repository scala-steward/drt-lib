package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.time.{MilliTimes, SDateLike}

object TouchdownModelAndFeatures {
  val targetName: String = "touchdown"
}

case class TouchdownModelAndFeatures(model: RegressionModel, features: FeaturesWithOneToManyValues, examplesTrainedOn: Int, improvementPct: Double) extends ModelAndFeatures {
  def maybePrediction(arrival: Arrival)(implicit sDateProvider: Long => SDateLike): Option[Long] = {
    val dow = s"dow_${sDateProvider(arrival.Scheduled).getDayOfWeek()}"
    val partOfDay = s"pod_${sDateProvider(arrival.Scheduled).getHours() / 12}"
    val dowIdx = features.oneToManyValues.indexOf(dow)
    val partOfDayIds = features.oneToManyValues.indexOf(partOfDay)
    for {
      dowCo <- model.coefficients.toIndexedSeq.lift(dowIdx)
      partOfDayCo <- model.coefficients.toIndexedSeq.lift(partOfDayIds)
    } yield {
      val offScheduled = (model.intercept + dowCo + partOfDayCo).toInt
      arrival.Scheduled + (offScheduled * MilliTimes.oneMinuteMillis)
    }
  }
}
