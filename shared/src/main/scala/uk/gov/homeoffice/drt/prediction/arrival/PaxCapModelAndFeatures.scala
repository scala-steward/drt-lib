package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, RegressionModel}

object PaxCapModelAndFeatures {
  val targetName: String = "paxcap"
}

case class PaxCapModelAndFeatures(model: RegressionModel,
                                  features: FeaturesWithOneToManyValues,
                                  examplesTrainedOn: Int,
                                  improvementPct: Double,
                                 ) extends ArrivalModelAndFeatures {
  override val targetName: String = PaxCapModelAndFeatures.targetName
}
