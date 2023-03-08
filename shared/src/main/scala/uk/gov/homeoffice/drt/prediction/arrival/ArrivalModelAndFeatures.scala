package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.prediction.ModelAndFeatures
import uk.gov.homeoffice.drt.time.SDateLike

trait ArrivalModelAndFeatures extends ModelAndFeatures {
  private def dayOfWeek(ts: Long)(implicit sDateProvider: Long => SDateLike): String = s"dow_${sDateProvider(ts).getDayOfWeek()}"
  private def amPm(ts: Long)(implicit sDateProvider: Long => SDateLike): String = s"pod_${sDateProvider(ts).getHours() / 12}"

  def prediction(arrival: Arrival)(implicit sDateProvider: Long => SDateLike): Option[Int] = {
    val dowIdx = features.oneToManyValues.indexOf(dayOfWeek(arrival.Scheduled))
    val partOfDayIds = features.oneToManyValues.indexOf(amPm(arrival.Scheduled))
    for {
      dowCo <- model.coefficients.toIndexedSeq.lift(dowIdx)
      partOfDayCo <- model.coefficients.toIndexedSeq.lift(partOfDayIds)
    } yield {
      (model.intercept + dowCo + partOfDayCo).toInt
    }
  }
}

