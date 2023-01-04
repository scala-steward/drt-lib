package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.time.SDateLike

object OffScheduleModelAndFeatures {
  val targetName: String = "off-schedule"
}

case class OffScheduleModelAndFeatures(model: RegressionModel,
                                       features: FeaturesWithOneToManyValues,
                                       examplesTrainedOn: Int,
                                       improvementPct: Double,
                                      ) extends ModelAndFeatures {
  override val targetName: String = OffScheduleModelAndFeatures.targetName

  def maybeOffScheduleMinutes(arrival: Arrival)(implicit sDateProvider: Long => SDateLike): Option[Int] = {
    val dow = s"dow_${sDateProvider(arrival.Scheduled).getDayOfWeek()}"
    val partOfDay = s"pod_${sDateProvider(arrival.Scheduled).getHours() / 12}"
    val dowIdx = features.oneToManyValues.indexOf(dow)
    val partOfDayIds = features.oneToManyValues.indexOf(partOfDay)
    for {
      dowCo <- model.coefficients.toIndexedSeq.lift(dowIdx)
      partOfDayCo <- model.coefficients.toIndexedSeq.lift(partOfDayIds)
    } yield {
      (model.intercept + dowCo + partOfDayCo).toInt
    }
  }
}
