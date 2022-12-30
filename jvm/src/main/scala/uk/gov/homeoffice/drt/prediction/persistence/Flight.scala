package uk.gov.homeoffice.drt.prediction.persistence

import akka.actor.ActorSystem
import akka.util.Timeout
import uk.gov.homeoffice.drt.actor.TerminalDateActor.FlightRoute
import uk.gov.homeoffice.drt.prediction.{ModelCategory, PersistenceImpl}
import uk.gov.homeoffice.drt.prediction.category.FlightCategory
import uk.gov.homeoffice.drt.time.SDateLike

import scala.concurrent.ExecutionContext

case class Flight(now: () => SDateLike)
                 (implicit
                  val ec: ExecutionContext,
                  val timeout: Timeout,
                  val system: ActorSystem
                 ) extends PersistenceImpl[FlightRoute] {
  override val modelCategory: ModelCategory = FlightCategory
}
