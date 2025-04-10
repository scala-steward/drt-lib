package uk.gov.homeoffice.drt.actor.commands

import org.apache.pekko.actor.ActorRef

object Commands {
  object GetState

  case class AddUpdatesSubscriber(source: ActorRef)

}
