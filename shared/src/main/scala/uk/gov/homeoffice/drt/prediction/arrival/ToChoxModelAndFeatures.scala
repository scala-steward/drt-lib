package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, RegressionModel}

object ToChoxModelAndFeatures {
  val targetName: String = "to-chox"
}

case class ToChoxModelAndFeatures(model: RegressionModel,
                                  features: FeaturesWithOneToManyValues,
                                  examplesTrainedOn: Int,
                                  improvementPct: Double) extends ArrivalModelAndFeatures {
  override val targetName: String = ToChoxModelAndFeatures.targetName
}
