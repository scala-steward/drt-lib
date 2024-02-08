package uk.gov.homeoffice.drt.prediction.persistence

import akka.actor.ActorSystem
import akka.util.Timeout
import uk.gov.homeoffice.drt.prediction.category.FlightCategory
import uk.gov.homeoffice.drt.prediction.{ModelCategory, ActorModelPersistenceImpl}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.ExecutionContext

case class Flight()
                 (implicit
                  val ec: ExecutionContext,
                  val timeout: Timeout,
                  val system: ActorSystem
                 ) extends ActorModelPersistenceImpl {
  override val now: () => SDateLike = () => SDate.now()
  override val modelCategory: ModelCategory = FlightCategory
}
