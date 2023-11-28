package uk.gov.homeoffice.drt.actor.commands

import akka.actor.ActorRef

object Commands {
  object GetState

  case class AddUpdatesSubscriber(source: ActorRef)

}
