package uk.gov.homeoffice.drt.prediction

import org.slf4j.LoggerFactory

trait ModelAndFeatures {
  val model: RegressionModel
  val features: FeaturesWithOneToManyValues
  val targetName: String
  val examplesTrainedOn: Int
  val improvementPct: Double
}

object ModelAndFeatures {
  private val log = LoggerFactory.getLogger(getClass)
  def apply(model: RegressionModel,
            features: FeaturesWithOneToManyValues,
            targetName: String,
            examplesTrainedOn: Int,
            improvementPct: Double,
           ): ModelAndFeatures = targetName match {
    case OffScheduleModelAndFeatures.targetName => OffScheduleModelAndFeatures(model, features, examplesTrainedOn, improvementPct)
    case ToChoxModelAndFeatures.targetName => ToChoxModelAndFeatures(model, features, examplesTrainedOn, improvementPct)
    case unknown =>
      throw new RuntimeException(s"Unrecognised model name: $unknown")
  }
}
