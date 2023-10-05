package uk.gov.homeoffice.drt.actor.serialisation

import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.ConfigActor
import uk.gov.homeoffice.drt.actor.ConfigActor.{RemoveConfig, SetUpdate}
import uk.gov.homeoffice.drt.actor.serialisation.SlasMessageConversion.{setSlasUpdatesFromMessage, slaConfigsFromMessage}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.SlaConfigs
import uk.gov.homeoffice.drt.ports.config.updates.Configs
import uk.gov.homeoffice.drt.protobuf.messages.SlasUpdates._
import uk.gov.homeoffice.drt.protobuf.messages.config.Configs.RemoveConfigMessage


trait ConfigDeserialiser[B, A <: Configs[B]] {
  def deserialiseCommand(a: scalapb.GeneratedMessage): ConfigActor.Command

  def deserialiseState(a: scalapb.GeneratedMessage): A

  def removeUpdate(a: RemoveConfigMessage): RemoveConfig =
    RemoveConfig(a.effectiveFrom.getOrElse(throw new Exception("No effectiveFrom in message")))
}

object ConfigDeserialiser {
  implicit val slaConfigsDeserialiser: ConfigDeserialiser[Map[Queue, Int], SlaConfigs] = new ConfigDeserialiser[Map[Queue, Int], SlaConfigs] {
    override def deserialiseCommand(a: GeneratedMessage): ConfigActor.Command = a match {
      case msg: SetSlasUpdateMessage => setSlasUpdatesFromMessage(msg).asInstanceOf[SetUpdate[Map[Queue, Int]]]
    }

    override def deserialiseState(a: GeneratedMessage): SlaConfigs = a match {
      case msg: SlaConfigsMessage => slaConfigsFromMessage(msg)
    }
  }
}
