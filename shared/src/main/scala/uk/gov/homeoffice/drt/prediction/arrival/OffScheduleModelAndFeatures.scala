package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, RegressionModel}

object OffScheduleModelAndFeatures {
  val targetName: String = "off-schedule"
  val featuresVersion: Int = 1
}

case class OffScheduleModelAndFeatures(model: RegressionModel,
                                       features: FeaturesWithOneToManyValues,
                                       examplesTrainedOn: Int,
                                       improvementPct: Double,
                                      ) extends ArrivalModelAndFeatures {
  override val featuresVersion: Int = OffScheduleModelAndFeatures.featuresVersion
  override val targetName: String = OffScheduleModelAndFeatures.targetName
}
