package uk.gov.homeoffice.drt.prediction

trait ModelAndFeatures {
  val model: RegressionModel
  val features: FeaturesWithOneToManyValues
  val targetName: String
  val examplesTrainedOn: Int
  val improvementPct: Double
}

object ModelAndFeatures {
  def apply(model: RegressionModel,
            features: FeaturesWithOneToManyValues,
            targetName: String,
            examplesTrainedOn: Int,
            improvementPct: Double,
           ): ModelAndFeatures = targetName match {
    case TouchdownModelAndFeatures.targetName => TouchdownModelAndFeatures(model, features, examplesTrainedOn, improvementPct)
    case ToChoxModelAndFeatures.targetName => ToChoxModelAndFeatures(model, features, examplesTrainedOn, improvementPct)
  }
}
