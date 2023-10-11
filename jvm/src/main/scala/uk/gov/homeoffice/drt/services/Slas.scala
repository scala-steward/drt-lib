package uk.gov.homeoffice.drt.services

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import uk.gov.homeoffice.drt.actor.commands.Commands.GetState
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.SlaConfigs
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

import scala.concurrent.{ExecutionContext, Future}

object Slas {
  def slaProvider(slasActor: ActorRef)
                 (implicit ec: ExecutionContext, timeout: Timeout): (LocalDate, Queue) => Future[Int] =
    (date, queue) =>
      slasActor
        .ask(GetState)
        .mapTo[SlaConfigs]
        .map {
          _.configForDate(SDate(date).millisSinceEpoch)
            .getOrElse(throw new Exception(s"No slas found for $date"))
            .getOrElse(queue, throw new Exception("No slas found for queue"))
        }

}
