package uk.gov.homeoffice.drt.protobuf.serialisation

import uk.gov.homeoffice.drt.prediction.Feature.{OneToMany, Single}
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{OneToManyFeatureColumn, SingleFeatureColumn}
import uk.gov.homeoffice.drt.prediction.{Feature, FeaturesWithOneToManyValues, ModelAndFeatures, RegressionModel}
import uk.gov.homeoffice.drt.protobuf.messages.ModelAndFeatures._
import uk.gov.homeoffice.drt.time.SDateLike

object ModelAndFeaturesConversion {
  def modelsAndFeaturesFromMessage(msg: ModelsAndFeaturesMessage)(implicit sdate: Long => SDateLike): Iterable[ModelAndFeatures] =
    msg.modelsAndFeatures.map(modelAndFeaturesFromMessage)

  def modelAndFeaturesFromMessage(msg: ModelAndFeaturesMessage)(implicit sdate: Long => SDateLike): ModelAndFeatures = {
    val model = msg.model.map(modelFromMessage).getOrElse(throw new Exception("No value for model"))
    val features = msg.features.map(featuresFromMessage).getOrElse(throw new Exception("No value for features"))
    val targetName = msg.targetName.getOrElse(throw new Exception("Mandatory parameter 'targetName' not specified"))
    val examplesTrainedOn = msg.examplesTrainedOn.getOrElse(throw new Exception("Mandatory parameter 'examplesTrainedOn' not specified"))
    val improvementPct = msg.improvementPct.getOrElse(throw new Exception("Mandatory parameter 'improvement' not specified"))

    ModelAndFeatures(model, features, targetName, examplesTrainedOn, improvementPct)
  }

  def modelFromMessage(msg: RegressionModelMessage): RegressionModel =
    RegressionModel(msg.coefficients, msg.intercept.getOrElse(throw new Exception("No value for intercept")))

  def featuresFromMessage(msg: FeaturesMessage)(implicit sdate: Long => SDateLike): FeaturesWithOneToManyValues = {
    val singles: Seq[Single] = msg.singleFeatures.map(c => Single(SingleFeatureColumn.fromLabel(c)))
    val oneToManys: Seq[Feature] = msg.oneToManyFeatures.map(oneToManyFromMessage)
    val allFeatures = oneToManys ++ singles

    FeaturesWithOneToManyValues(allFeatures.toList, msg.oneToManyValues.toIndexedSeq)
  }

  def oneToManyFromMessage(msg: OneToManyFeatureMessage)(implicit sdate: Long => SDateLike): Feature = {
    for {
      prefix <- msg.prefix
      column <- msg.column
    } yield {
      OneToMany(OneToManyFeatureColumn.fromLabel(column), prefix)
    }
  }.getOrElse(throw new Exception("No value for prefix or column"))

  def modelToMessage(model: RegressionModel): RegressionModelMessage =
    RegressionModelMessage(
      coefficients = model.coefficients.toSeq,
      intercept = Option(model.intercept),
    )

  def featuresToMessage(features: FeaturesWithOneToManyValues): FeaturesMessage = {
    FeaturesMessage(
      oneToManyFeatures = features.features.collect {
        case OneToMany(column, prefix) =>
          OneToManyFeatureMessage(
            prefix = Option(prefix),
            column = Option(column.label)
          )
      },
      singleFeatures = features.features.collect {
        case Single(columnName) => columnName.label
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
