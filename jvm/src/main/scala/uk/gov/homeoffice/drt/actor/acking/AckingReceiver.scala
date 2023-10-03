package uk.gov.homeoffice.drt.actor.acking

import akka.actor.ActorRef
import akka.pattern.StatusReply.Ack
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext

object AckingReceiver {

  case object StreamInitialized

  case object StreamCompleted

  final case class StreamFailure(ex: Throwable)

}

object Acking {
  type AckingAsker = (ActorRef, Any, ActorRef) => Unit

  def askThenAck(implicit ec: ExecutionContext, timeout: Timeout): AckingAsker =
    (actor: ActorRef, message: Any, replyTo: ActorRef) =>
      actor.ask(message).foreach(_ => replyTo ! Ack)
}
