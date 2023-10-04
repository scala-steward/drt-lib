package uk.gov.homeoffice.drt.actor.serialisation

import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.ConfigActor.{RemoveUpdate, SetUpdate}
import uk.gov.homeoffice.drt.actor.serialisation.SlasMessageConversion.{setSlasUpdatesToMessage, slasUpdatesToMessage}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.Slas
import uk.gov.homeoffice.drt.ports.config.updates.UpdatesWithHistory
import uk.gov.homeoffice.drt.protobuf.messages.config.ConfigUpdates.RemoveUpdateMessage

trait ConfigSerialiser[B, A <: UpdatesWithHistory[B]] {
  def updatesWithHistory(a: A): scalapb.GeneratedMessage

  def setUpdate(a: SetUpdate[B]): scalapb.GeneratedMessage

  def removeUpdate(a: RemoveUpdate): scalapb.GeneratedMessage = RemoveUpdateMessage(Option(a.effectiveFrom))
}

object ConfigSerialiser {
  implicit val setSlasUpdateSerialiser: ConfigSerialiser[Map[Queue, Int], Slas] = new ConfigSerialiser[Map[Queue, Int], Slas] {
    override def updatesWithHistory(a: Slas): GeneratedMessage = slasUpdatesToMessage(a)

    override def setUpdate(a: SetUpdate[Map[Queue, Int]]): GeneratedMessage = setSlasUpdatesToMessage(a)
  }
}
