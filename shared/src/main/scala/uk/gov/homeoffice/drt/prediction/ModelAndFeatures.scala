package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.prediction.arrival.{OffScheduleModelAndFeatures, ToChoxModelAndFeatures, WalkTimeModelAndFeatures}

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
    case OffScheduleModelAndFeatures.targetName => OffScheduleModelAndFeatures(model, features, examplesTrainedOn, improvementPct)
    case ToChoxModelAndFeatures.targetName => ToChoxModelAndFeatures(model, features, examplesTrainedOn, improvementPct)
    case WalkTimeModelAndFeatures.targetName => WalkTimeModelAndFeatures(model, features, examplesTrainedOn, improvementPct)
    case unknown =>
      throw new RuntimeException(s"Unrecognised model name: $unknown")
  }
}
