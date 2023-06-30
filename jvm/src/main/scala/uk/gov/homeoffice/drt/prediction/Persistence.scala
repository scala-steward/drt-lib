package uk.gov.homeoffice.drt.prediction

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import uk.gov.homeoffice.drt.actor.PredictionModelActor
import uk.gov.homeoffice.drt.actor.PredictionModelActor.{ModelUpdate, Models, RemoveModel, WithId}
import uk.gov.homeoffice.drt.actor.TerminalDateActor.GetState
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.{ExecutionContext, Future}

trait Persistence {
  val modelCategory: ModelCategory
  val now: () => SDateLike
  val actorProvider: (ModelCategory, WithId) => ActorRef

  implicit val ec: ExecutionContext
  implicit val timeout: Timeout
  implicit val system: ActorSystem

  val updateModel: (WithId, String, Option[ModelUpdate]) => Future[_] =
    (identifier, modelName, maybeModelUpdate) => {
      val actor = actorProvider(modelCategory, identifier)
      val msg = maybeModelUpdate match {
        case Some(modelUpdate) => modelUpdate
        case None => RemoveModel(modelName)
      }
      actor.ask(msg).map(_ => actor ! PoisonPill)
    }

  def getModels(validModelNames: Seq[String]): WithId => Future[Models] =
    identifier => {
      val actor = actorProvider(modelCategory, identifier)
      actor
        .ask(GetState).mapTo[Models]
        .map { models =>
          actor ! PoisonPill
          Models(models.models.view.filterKeys(validModelNames.contains).toMap)
        }
    }
}

trait PersistenceImpl extends Persistence {
  override val actorProvider: (ModelCategory, WithId) => ActorRef =
    (modelCategory, identifier) => system.actorOf(Props(new PredictionModelActor(() => SDate.now(), modelCategory, identifier)))
}
