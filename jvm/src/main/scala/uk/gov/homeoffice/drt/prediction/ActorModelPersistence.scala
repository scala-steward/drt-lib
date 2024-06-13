package uk.gov.homeoffice.drt.prediction

import akka.Done
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.apache.spark.ml.regression.LinearRegressionModel
import uk.gov.homeoffice.drt.actor.PredictionModelActor
import uk.gov.homeoffice.drt.actor.PredictionModelActor._
import uk.gov.homeoffice.drt.actor.commands.Commands.GetState
import uk.gov.homeoffice.drt.time.SDate

import scala.concurrent.{ExecutionContext, Future}


trait ModelPersistence {
  def getModels(validModelNames: Seq[String], maybePointInTime: Option[Long]): WithId => Future[Models]
  val persist: (WithId, Int, LinearRegressionModel, FeaturesWithOneToManyValues, Int, Double, String) => Future[Done]
  val clear: (WithId, String) => Future[Done]
}

trait ActorModelPersistence extends ModelPersistence {
  val modelCategory: ModelCategory
  val actorProvider: (ModelCategory, WithId, Option[Long]) => ActorRef =
    (modelCategory, identifier, maybePointInTime) => system.actorOf(Props(new PredictionModelActor(() => SDate.now(), modelCategory, identifier, maybePointInTime)))

  implicit val ec: ExecutionContext
  implicit val timeout: Timeout
  implicit val system: ActorSystem

  private val updateModel: (WithId, String, Option[ModelUpdate]) => Future[Done] =
    (identifier, modelName, maybeModelUpdate) => {
      val actor = actorProvider(modelCategory, identifier, None)
      val msg = maybeModelUpdate match {
        case Some(modelUpdate) => modelUpdate
        case None => RemoveModel(modelName)
      }
      actor.ask(msg).map { _ =>
        actor ! PoisonPill
        Done
      }
    }

  override def getModels(validModelNames: Seq[String], maybePointInTime: Option[Long]): WithId => Future[Models] =
    identifier => {
      val actor = actorProvider(modelCategory, identifier, maybePointInTime)
      actor
        .ask(GetState).mapTo[Models]
        .map { models =>
          actor ! PoisonPill
          Models(models.models.view.filterKeys(validModelNames.contains).toMap)
        }
    }

  override val persist: (WithId, Int, LinearRegressionModel, FeaturesWithOneToManyValues, Int, Double, String) => Future[Done] =
    (modelIdentifier, featuresVersion, linearRegressionModel, featuresWithValues, trainingExamples, improvementPct, modelName) => {
      val regressionModel = RegressionModelFromSpark(linearRegressionModel)
      val modelUpdate = ModelUpdate(regressionModel, featuresVersion, featuresWithValues, trainingExamples, improvementPct, modelName)
      updateModel(modelIdentifier, modelName, Option(modelUpdate)).map(_ => Done)
    }

  override val clear: (WithId, String) => Future[Done] =
    (modelIdentifier, modelName) => {
      updateModel(modelIdentifier, modelName, None)
    }
}
