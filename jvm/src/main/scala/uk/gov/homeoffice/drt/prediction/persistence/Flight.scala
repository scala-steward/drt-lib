package uk.gov.homeoffice.drt.prediction.persistence

import akka.actor.ActorSystem
import akka.util.Timeout
import uk.gov.homeoffice.drt.prediction.category.FlightCategory
import uk.gov.homeoffice.drt.prediction.{ActorModelPersistence, ModelCategory}

import scala.concurrent.ExecutionContext

case class Flight()
                 (implicit
                  val ec: ExecutionContext,
                  val timeout: Timeout,
                  val system: ActorSystem
                 ) extends ActorModelPersistence {
  override val modelCategory: ModelCategory = FlightCategory
}
