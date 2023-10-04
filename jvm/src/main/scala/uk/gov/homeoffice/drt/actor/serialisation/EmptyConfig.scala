package uk.gov.homeoffice.drt.actor.serialisation

import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.Slas
import uk.gov.homeoffice.drt.ports.config.updates.UpdatesWithHistory

trait EmptyConfig[B, A <: UpdatesWithHistory[B]] {
  def empty: A
}

object EmptyConfig {
  implicit val emptySlaUpdates: EmptyConfig[Map[Queue, Int], Slas] = new EmptyConfig[Map[Queue, Int], Slas] {
    override def empty: Slas = Slas.empty
  }
}

