package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.arrivals.Arrival
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
           ): ModelAndFeatures = (targetName, features) match {
    case (OffScheduleModelAndFeatures.targetName, fts: FeaturesWithOneToManyValues) =>
      OffScheduleModelAndFeatures(model, fts, examplesTrainedOn, improvementPct)
    case (ToChoxModelAndFeatures.targetName, fts: FeaturesWithOneToManyValues) =>
      ToChoxModelAndFeatures(model, fts, examplesTrainedOn, improvementPct)
    case (WalkTimeModelAndFeatures.targetName, fts: FeaturesWithOneToManyValues) =>
      WalkTimeModelAndFeatures(model, fts, examplesTrainedOn, improvementPct)
    case unknown =>
      throw new RuntimeException(s"Unrecognised model name: $unknown")
  }
}
