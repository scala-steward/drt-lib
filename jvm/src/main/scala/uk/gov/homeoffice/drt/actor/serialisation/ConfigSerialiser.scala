package uk.gov.homeoffice.drt.actor.serialisation

import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.ConfigActor.{RemoveConfig, SetUpdate}
import uk.gov.homeoffice.drt.actor.serialisation.SlasMessageConversion.{setSlasUpdatesToMessage, slaConfigsToMessage}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.SlaConfigs
import uk.gov.homeoffice.drt.ports.config.updates.Configs
import uk.gov.homeoffice.drt.protobuf.messages.config.Configs.RemoveConfigMessage

trait ConfigSerialiser[B, A <: Configs[B]] {
  def updatesWithHistory(a: A): scalapb.GeneratedMessage

  def setUpdate(a: SetUpdate[B], now: Long): scalapb.GeneratedMessage

  def removeUpdate(a: RemoveConfig, createdAt: Long): RemoveConfigMessage = RemoveConfigMessage(Option(a.effectiveFrom), Option(createdAt))
}

object ConfigSerialiser {
  implicit val slaConfigsSerialiser: ConfigSerialiser[Map[Queue, Int], SlaConfigs] = new ConfigSerialiser[Map[Queue, Int], SlaConfigs] {
    override def updatesWithHistory(a: SlaConfigs): GeneratedMessage = slaConfigsToMessage(a)

    override def setUpdate(a: SetUpdate[Map[Queue, Int]], createdAt: Long): GeneratedMessage = setSlasUpdatesToMessage(a, createdAt)
  }
}
