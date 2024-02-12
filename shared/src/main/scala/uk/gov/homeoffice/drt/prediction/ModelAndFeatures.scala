package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.prediction.arrival._

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
           ): ModelAndFeatures = (targetName, features) match {
    case (OffScheduleModelAndFeatures.targetName, fts: FeaturesWithOneToManyValues) =>
      OffScheduleModelAndFeatures(model, fts, examplesTrainedOn, improvementPct)
    case (ToChoxModelAndFeatures.targetName, fts: FeaturesWithOneToManyValues) =>
      ToChoxModelAndFeatures(model, fts, examplesTrainedOn, improvementPct)
    case (WalkTimeModelAndFeatures.targetName, fts: FeaturesWithOneToManyValues) =>
      WalkTimeModelAndFeatures(model, fts, examplesTrainedOn, improvementPct)
    case (PaxCapModelAndFeatures.targetName, fts: FeaturesWithOneToManyValues) =>
      PaxCapModelAndFeatures(model, fts, examplesTrainedOn, improvementPct)
    case (PaxCapModelAndFeaturesV2.targetName, fts: FeaturesWithOneToManyValues) =>
      PaxCapModelAndFeaturesV2(model, fts, examplesTrainedOn, improvementPct)
    case unknown =>
      throw new RuntimeException(s"Unrecognised model name: $unknown")
  }
}
