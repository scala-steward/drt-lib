package uk.gov.homeoffice.drt.actor

import org.apache.spark.ml.regression.LinearRegressionModel
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.PredictionModelActor.{Ack, Models, RemoveModel, WithId}
import uk.gov.homeoffice.drt.actor.TerminalDateActor.GetState
import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, ModelAndFeatures, ModelCategory, RegressionModel}
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

  trait WithId {
    val id: String
  }

  case class TerminalFlightNumberOrigin(terminal: String, number: Int, origin: String) extends WithId {
    val id = s"$terminal-$number-$origin"
  }

  case class TerminalCarrierOrigin(terminal: String, carrier: String, origin: String) extends WithId {
    val id = s"$terminal-$carrier-$origin"
  }
}

class PredictionModelActor(val now: () => SDateLike,
                           modelCategory: ModelCategory,
                           identifier: WithId,
                          ) extends RecoveryActorLike {

  import uk.gov.homeoffice.drt.protobuf.serialisation.ModelAndFeaturesConversion._

  override val log: Logger = LoggerFactory.getLogger(getClass)

  override val recoveryStartMillis: Long = now().millisSinceEpoch
  override val maybeSnapshotInterval: Option[Int] = Option(100)

  var state: Map[String, ModelAndFeatures] = Map()

  override def persistenceId: String = s"${modelCategory.name}-prediction-${identifier.id}".toLowerCase

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


