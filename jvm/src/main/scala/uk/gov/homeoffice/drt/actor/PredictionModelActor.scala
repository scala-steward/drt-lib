package uk.gov.homeoffice.drt.actor

import org.apache.spark.ml.regression.LinearRegressionModel
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.PredictionModelActor.{Ack, Models, RemoveModel}
import uk.gov.homeoffice.drt.actor.TerminalDateActor.{GetState, WithId}
import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, ModelAndFeatures, RegressionModel}
import uk.gov.homeoffice.drt.protobuf.messages.ModelAndFeatures.{ModelAndFeaturesMessage, ModelsAndFeaturesMessage, RemoveModelMessage}
import uk.gov.homeoffice.drt.time.SDateLike

object PredictionModelActor {
  case object Ack

  case class Models(models: Map[String, ModelAndFeatures])

  case class ModelUpdate(model: RegressionModel,
                         features: FeaturesWithOneToManyValues,
                         examplesTrainedOn: Int,
                         improvementPct: Double,
                         targetName: String,
                        ) extends ModelAndFeatures

  case class RemoveModel(targetName: String)

  object RegressionModelFromSpark {
    def apply(lrModel: LinearRegressionModel): RegressionModel = RegressionModel(lrModel.coefficients.toArray, lrModel.intercept)
  }
}

class PredictionModelActor(val now: () => SDateLike,
                           persistenceType: String,
                           identifier: WithId,
                          ) extends RecoveryActorLike {

  import uk.gov.homeoffice.drt.protobuf.serialisers.ModelAndFeaturesConversion._

  override val log: Logger = LoggerFactory.getLogger(getClass)

  override val recoveryStartMillis: Long = now().millisSinceEpoch

  var state: Map[String, ModelAndFeatures] = Map()

  override def persistenceId: String = s"$persistenceType-prediction-${identifier.id}".toLowerCase

  override def processRecoveryMessage: PartialFunction[Any, Unit] = {
    case RemoveModelMessage(targetName, _) =>
      targetName.foreach(tn => state = state - tn)

    case msg: ModelAndFeaturesMessage =>
      val modelAndFeatures = modelAndFeaturesFromMessage(msg)
      state = state.updated(modelAndFeatures.targetName, modelAndFeatures)
  }

  override def processSnapshotMessage: PartialFunction[Any, Unit] = {
    case msg: ModelsAndFeaturesMessage =>
      state = modelsAndFeaturesFromMessage(msg).map(maf => maf.targetName -> maf).toMap
  }

  override def stateToMessage: GeneratedMessage =
    modelsAndFeaturesToMessage(state.values, now().millisSinceEpoch)

  override def receiveCommand: Receive = {
    case GetState =>
      sender() ! Models(state)

    case maf: ModelAndFeatures =>
      val isUpdated = state.get(maf.targetName) match {
        case Some(existingMaf) => maf != existingMaf
        case None => true
      }
      if (isUpdated) {
        state = state.updated(maf.targetName, maf)
        val replyToAndAck = List((sender(), Ack))
        persistAndMaybeSnapshotWithAck(modelAndFeaturesToMessage(maf, now().millisSinceEpoch), replyToAndAck)
      }

    case RemoveModel(targetName) =>
      if (state.contains(targetName)) {
        state = state - targetName
        val replyToAndAck = List((sender(), Ack))
        persistAndMaybeSnapshotWithAck(RemoveModelMessage(Option(targetName), Option(now().millisSinceEpoch)), replyToAndAck)
      }
  }
}


