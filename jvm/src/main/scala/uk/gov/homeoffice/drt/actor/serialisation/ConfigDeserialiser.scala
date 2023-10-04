package uk.gov.homeoffice.drt.actor.serialisation

import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.ConfigActor
import uk.gov.homeoffice.drt.actor.ConfigActor.{RemoveUpdate, SetUpdate}
import uk.gov.homeoffice.drt.actor.serialisation.SlasMessageConversion.{setSlasUpdatesFromMessage, slasUpdatesFromMessage}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.Slas
import uk.gov.homeoffice.drt.ports.config.updates.UpdatesWithHistory
import uk.gov.homeoffice.drt.protobuf.messages.SlasUpdates._
import uk.gov.homeoffice.drt.protobuf.messages.config.ConfigUpdates.RemoveUpdateMessage


trait ConfigDeserialiser[B, A <: UpdatesWithHistory[B]] {
  def deserialiseCommand(a: scalapb.GeneratedMessage): ConfigActor.Command
  def deserialiseState(a: scalapb.GeneratedMessage): A
}

object ConfigDeserialiser {
  implicit val slasDeserialiser: ConfigDeserialiser[Map[Queue, Int], Slas] = new ConfigDeserialiser[Map[Queue, Int], Slas] {
    override def deserialiseCommand(a: GeneratedMessage): ConfigActor.Command = a match {
      case msg: SetSlasUpdateMessage => setSlasUpdatesFromMessage(msg).asInstanceOf[SetUpdate[Map[Queue, Int]]]
      case msg: RemoveUpdateMessage => RemoveUpdate(msg.effectiveFrom.getOrElse(0L))
    }

    override def deserialiseState(a: GeneratedMessage): Slas = a match {
      case msg: SlaUpdatesMessage => slasUpdatesFromMessage(msg)
    }
  }
}
