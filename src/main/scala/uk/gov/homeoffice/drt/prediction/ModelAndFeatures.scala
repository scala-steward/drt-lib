package uk.gov.homeoffice.drt.prediction

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
           ): ModelAndFeatures = targetName match {
    case TouchdownModelAndFeatures.targetName => TouchdownModelAndFeatures(model, features, examplesTrainedOn, improvementPct)
  }
}
