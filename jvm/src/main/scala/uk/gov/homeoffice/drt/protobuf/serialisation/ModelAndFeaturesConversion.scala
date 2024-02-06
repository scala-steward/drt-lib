package uk.gov.homeoffice.drt.protobuf.serialisation

import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{OneToMany, Single}
import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, ModelAndFeatures, RegressionModel}
import uk.gov.homeoffice.drt.protobuf.messages.ModelAndFeatures._
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

import scala.util.{Failure, Success, Try}

object ModelAndFeaturesConversion {
  private val log = LoggerFactory.getLogger(getClass)

  def modelsAndFeaturesFromMessage[T](msg: ModelsAndFeaturesMessage)
                                     (implicit sdate: Long => SDateLike,
                                      sdateFromLocalDate: LocalDate => SDateLike): Iterable[ModelAndFeatures] =
    msg.modelsAndFeatures
      .map(modelAndFeaturesFromMessage)
      .collect { case Some(maf) => maf }

  def modelAndFeaturesFromMessage[T](msg: ModelAndFeaturesMessage)
                                    (implicit sdate: Long => SDateLike,
                                     sdateFromLocalDate: LocalDate => SDateLike): Option[ModelAndFeatures] = {
    val model = msg.model.map(modelFromMessage).getOrElse(throw new Exception("No value for model"))
    val features = msg.features.map(featuresFromMessage).getOrElse(throw new Exception("No value for features"))
    val targetName = msg.targetName.getOrElse(throw new Exception("Mandatory parameter 'targetName' not specified"))
    val examplesTrainedOn = msg.examplesTrainedOn.getOrElse(throw new Exception("Mandatory parameter 'examplesTrainedOn' not specified"))
    val improvementPct = msg.improvementPct.getOrElse(throw new Exception("Mandatory parameter 'improvement' not specified"))

    Try(ModelAndFeatures(model, features, targetName, examplesTrainedOn, improvementPct)) match {
      case Success(maf) => Option(maf)
      case Failure(exception) =>
        log.warn(s"Failed to deserialise ModelAndFeatures: ${exception.getMessage}")
        None
    }
  }

  def modelFromMessage(msg: RegressionModelMessage): RegressionModel =
    RegressionModel(msg.coefficients, msg.intercept.getOrElse(throw new Exception("No value for intercept")))

  def featuresFromMessage(msg: FeaturesMessage)
                         (implicit
                          sdateFromLong: Long => SDateLike,
                          sdateFromLocalDate: LocalDate => SDateLike,
                         ): FeaturesWithOneToManyValues = {
    implicit val now: () => SDate.JodaSDate = () => SDate.now()
    val singles = msg.singleFeatures
      .map(l => Try(Single.fromLabel(l)))
    val oneToManys = msg.oneToManyFeatures
      .map(l => Try(OneToMany.fromLabel(l)))

    val allFeatures = (oneToManys ++ singles).collect { case Success(f) => f }

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
