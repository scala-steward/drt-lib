package uk.gov.homeoffice.drt.actor

import akka.actor.Actor
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.protobuf.messages.CrunchState.FlightWithSplitsMessage
import uk.gov.homeoffice.drt.time.UtcDate


trait TerminalDateActor extends Actor {
  val terminal: Terminal
  val date: UtcDate
  val extractValues: FlightWithSplitsMessage => Option[(Double, Seq[String])]
}

object TerminalDateActor {
  case object GetState

  case class ArrivalKey(scheduled: Long, terminal: String, number: Int)

  case class ArrivalKeyWithOrigin(scheduled: Long, terminal: String, number: Int, origin: String)
}
