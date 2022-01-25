package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.time.SDateLike

trait ModelAndFeatures {
  val model: RegressionModel
  val features: Features
  val examplesTrainedOn: Int
  val improvementPct: Double
}

object ModelAndFeatures {
  def apply(model: RegressionModel,
            features: Features,
            targetName: String,
            examplesTrainedOn: Int,
            improvementPct: Double,
            sDateProvider: Long => SDateLike,
           ): ModelAndFeatures = targetName match {
    case TouchdownModelAndFeatures.targetName => TouchdownModelAndFeatures(model, features, examplesTrainedOn, improvementPct, sDateProvider)
  }
}
