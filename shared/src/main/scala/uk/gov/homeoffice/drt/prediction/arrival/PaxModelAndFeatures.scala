package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, RegressionModel}

object PaxModelAndFeatures {
  val targetName: String = "pax"
}

case class PaxModelAndFeatures(model: RegressionModel,
                               features: FeaturesWithOneToManyValues,
                               examplesTrainedOn: Int,
                               improvementPct: Double,
                              ) extends ArrivalModelAndFeatures {
  override val targetName: String = PaxModelAndFeatures.targetName
}
