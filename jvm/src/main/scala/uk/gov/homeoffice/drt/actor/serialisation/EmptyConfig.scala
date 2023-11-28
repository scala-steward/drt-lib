package uk.gov.homeoffice.drt.actor.serialisation

import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.SlaConfigs
import uk.gov.homeoffice.drt.ports.config.updates.Configs

trait EmptyConfig[B, A <: Configs[B]] {
  def empty: A
}

object EmptyConfig {
  implicit val emptySlaUpdates: EmptyConfig[Map[Queue, Int], SlaConfigs] = new EmptyConfig[Map[Queue, Int], SlaConfigs] {
    override def empty: SlaConfigs = SlaConfigs.empty
  }
}

