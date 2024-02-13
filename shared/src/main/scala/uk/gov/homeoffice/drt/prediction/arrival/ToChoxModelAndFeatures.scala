package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, RegressionModel}

object ToChoxModelAndFeatures {
  val targetName: String = "to-chox"
  val featuresVersion: Int = 1
}

case class ToChoxModelAndFeatures(model: RegressionModel,
                                  features: FeaturesWithOneToManyValues,
                                  examplesTrainedOn: Int,
                                  improvementPct: Double) extends ArrivalModelAndFeatures {
  override val featuresVersion: Int = ToChoxModelAndFeatures.featuresVersion
  override val targetName: String = ToChoxModelAndFeatures.targetName
}
