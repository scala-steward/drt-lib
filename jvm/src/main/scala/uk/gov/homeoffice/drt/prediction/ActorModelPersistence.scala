package uk.gov.homeoffice.drt.prediction

import akka.Done
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.apache.spark.ml.regression.LinearRegressionModel
import uk.gov.homeoffice.drt.actor.PredictionModelActor
import uk.gov.homeoffice.drt.actor.PredictionModelActor.{ModelUpdate, Models, RegressionModelFromSpark, RemoveModel, WithId}
import uk.gov.homeoffice.drt.actor.commands.Commands.GetState
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.{ExecutionContext, Future}


trait ModelPersistence {
  def getModels(validModelNames: Seq[String]): WithId => Future[Models]
  val persist: (WithId, LinearRegressionModel, FeaturesWithOneToManyValues, Int, Double, String) => Future[Done]
  val clear: (WithId, String) => Future[Done]
}

trait ActorModelPersistence extends ModelPersistence {
  val modelCategory: ModelCategory
  val now: () => SDateLike
  val actorProvider: (ModelCategory, WithId) => ActorRef

  implicit val ec: ExecutionContext
  implicit val timeout: Timeout
  implicit val system: ActorSystem

  private val updateModel: (WithId, String, Option[ModelUpdate]) => Future[Done] =
    (identifier, modelName, maybeModelUpdate) => {
      val actor = actorProvider(modelCategory, identifier)
      val msg = maybeModelUpdate match {
        case Some(modelUpdate) => modelUpdate
        case None => RemoveModel(modelName)
      }
      actor.ask(msg).map { _ =>
        actor ! PoisonPill
        Done
      }
    }

  override def getModels(validModelNames: Seq[String]): WithId => Future[Models] =
    identifier => {
      val actor = actorProvider(modelCategory, identifier)
      actor
        .ask(GetState).mapTo[Models]
        .map { models =>
          actor ! PoisonPill
          Models(models.models.view.filterKeys(validModelNames.contains).toMap)
        }
    }

  override val persist: (WithId, LinearRegressionModel, FeaturesWithOneToManyValues, Int, Double, String) => Future[Done] =
    (modelIdentifier, linearRegressionModel, featuresWithValues, trainingExamples, improvementPct, modelName) => {
      val regressionModel = RegressionModelFromSpark(linearRegressionModel)
      val modelUpdate = ModelUpdate(regressionModel, featuresWithValues, trainingExamples, improvementPct, modelName)
      updateModel(modelIdentifier, modelName, Option(modelUpdate)).map(_ => Done)
    }

  override val clear: (WithId, String) => Future[Done] =
    (modelIdentifier, modelName) => {
      updateModel(modelIdentifier, modelName, None)
    }
}

trait ActorModelPersistenceImpl extends ActorModelPersistence {
  override val actorProvider: (ModelCategory, WithId) => ActorRef =
    (modelCategory, identifier) => system.actorOf(Props(new PredictionModelActor(() => SDate.now(), modelCategory, identifier)))
}
