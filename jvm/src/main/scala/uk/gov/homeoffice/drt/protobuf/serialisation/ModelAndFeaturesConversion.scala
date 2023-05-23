package uk.gov.homeoffice.drt.protobuf.serialisation

import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{OneToMany, Single}
import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, ModelAndFeatures, RegressionModel}
import uk.gov.homeoffice.drt.protobuf.messages.ModelAndFeatures._
import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}

object ModelAndFeaturesConversion {
  def modelsAndFeaturesFromMessage[T](msg: ModelsAndFeaturesMessage)
                                     (implicit sdate: Long => SDateLike,
                                      sdateFromLocalDate: LocalDate => SDateLike): Iterable[ModelAndFeatures] =
    msg.modelsAndFeatures.map(modelAndFeaturesFromMessage)

  def modelAndFeaturesFromMessage[T](msg: ModelAndFeaturesMessage)
                                    (implicit sdate: Long => SDateLike,
                                     sdateFromLocalDate: LocalDate => SDateLike): ModelAndFeatures = {
    val model = msg.model.map(modelFromMessage).getOrElse(throw new Exception("No value for model"))
    val features = msg.features.map(featuresFromMessage).getOrElse(throw new Exception("No value for features"))
    val targetName = msg.targetName.getOrElse(throw new Exception("Mandatory parameter 'targetName' not specified"))
    val examplesTrainedOn = msg.examplesTrainedOn.getOrElse(throw new Exception("Mandatory parameter 'examplesTrainedOn' not specified"))
    val improvementPct = msg.improvementPct.getOrElse(throw new Exception("Mandatory parameter 'improvement' not specified"))

    ModelAndFeatures(model, features, targetName, examplesTrainedOn, improvementPct)
  }

  def modelFromMessage(msg: RegressionModelMessage): RegressionModel =
    RegressionModel(msg.coefficients, msg.intercept.getOrElse(throw new Exception("No value for intercept")))

  def featuresFromMessage(msg: FeaturesMessage)
                         (implicit
                          sdateFromLong: Long => SDateLike,
                          sdateFromLocalDate: LocalDate => SDateLike,
                         ): FeaturesWithOneToManyValues = {
    val singles = msg.singleFeatures.map(Single.fromLabel)
    val oneToManys = msg.oneToManyFeatures.map(OneToMany.fromLabel)
    val allFeatures = oneToManys ++ singles

    FeaturesWithOneToManyValues(allFeatures.toList, msg.oneToManyValues.toIndexedSeq)
  }

  def modelToMessage(model: RegressionModel): RegressionModelMessage =
    RegressionModelMessage(
      coefficients = model.coefficients.toSeq,
      intercept = Option(model.intercept),
    )

  def featuresToMessage(features: FeaturesWithOneToManyValues): FeaturesMessage = {
    FeaturesMessage(
      oneToManyFeatures = features.features.collect {
        case feature: OneToMany[_] => feature.label
      },
      singleFeatures = features.features.collect {
        case feature: Single[_] => feature.label
      },
      oneToManyValues = features.oneToManyValues
    )
  }

  def modelAndFeaturesToMessage(modelAndFeatures: ModelAndFeatures, now: Long): ModelAndFeaturesMessage = {
    ModelAndFeaturesMessage(
      model = Option(modelToMessage(modelAndFeatures.model)),
      features = Option(featuresToMessage(modelAndFeatures.features)),
      targetName = Option(modelAndFeatures.targetName),
      examplesTrainedOn = Option(modelAndFeatures.examplesTrainedOn),
      improvementPct = Option(modelAndFeatures.improvementPct),
      timestamp = Option(now),
    )
  }

  def modelsAndFeaturesToMessage(models: Iterable[ModelAndFeatures], now: Long): ModelsAndFeaturesMessage =
    ModelsAndFeaturesMessage(models.map(modelAndFeaturesToMessage(_, now)).toSeq)
}
