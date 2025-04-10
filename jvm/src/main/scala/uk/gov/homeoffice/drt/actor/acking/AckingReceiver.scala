package uk.gov.homeoffice.drt.actor.acking

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.pattern.StatusReply.Ack
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout

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
