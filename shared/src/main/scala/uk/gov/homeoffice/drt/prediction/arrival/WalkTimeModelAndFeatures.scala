package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, RegressionModel}

object WalkTimeModelAndFeatures {
  val targetName: String = "walk-time"
  val featuresVersion: Int = 1
}

case class WalkTimeModelAndFeatures(model: RegressionModel,
                                    features: FeaturesWithOneToManyValues,
                                    examplesTrainedOn: Int,
                                    improvementPct: Double) extends ArrivalModelAndFeatures {
  override val featuresVersion: Int = WalkTimeModelAndFeatures.featuresVersion
  override val targetName: String = WalkTimeModelAndFeatures.targetName
}
