package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, ModelAndFeatures, RegressionModel}

object WalkTimeModelAndFeatures {
  val targetName: String = "walk-time"
}

case class WalkTimeModelAndFeatures(model: RegressionModel,
                                    features: FeaturesWithOneToManyValues,
                                    examplesTrainedOn: Int,
                                    improvementPct: Double) extends ModelAndFeatures {
  override val targetName: String = WalkTimeModelAndFeatures.targetName
}
