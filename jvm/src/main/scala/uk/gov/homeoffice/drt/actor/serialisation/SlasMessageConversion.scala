package uk.gov.homeoffice.drt.actor.serialisation

import uk.gov.homeoffice.drt.actor.SetSlasUpdate
import uk.gov.homeoffice.drt.actor.SlasActor.{RemoveSlasUpdate, SetSlasUpdate}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.config.slas.SlasUpdate
import uk.gov.homeoffice.drt.protobuf.messages.SlasUpdates._
import uk.gov.homeoffice.drt.protobuf.messages.SlasUpdates.RemoveSlasUpdateMessage

object SlasMessageConversion {
  def removeSlasUpdateToMessage(delete: RemoveSlasUpdate): RemoveSlasUpdateMessage =
    RemoveSlasUpdateMessage(Option(delete.effectiveFrom))

  def removeSlasUpdatesFromMessage(delete: RemoveSlasUpdateMessage): RemoveSlasUpdate =
    RemoveSlasUpdate(delete.effectiveFrom.getOrElse(0L))

  def slasUpdateToMessage(slasUpdate: SlasUpdate): SlasUpdateMessage =
    SlasUpdateMessage(
      effectiveFrom = Option(slasUpdate.effectiveFrom),
      queueSlas = slasUpdate.item.map {
        case (queue, slas) => SlasMessage(Option(queue.toString), Option(slas))
      }.toSeq
    )

  def slasUpdateFromMessage(message: SlasUpdateMessage): SlasUpdate = {
    val queueSlas = message.queueSlas.map { slasMessage =>
      val queue = slasMessage.queue.getOrElse(throw new Exception("No queue in message"))
      val slaMinutes = slasMessage.minutes.getOrElse(throw new Exception("No sla minutes in message"))
      (Queue(queue), slaMinutes)
    }

    SlasUpdate(
      effectiveFrom = message.effectiveFrom.getOrElse(throw new Exception("No effectiveFrom in message")),
      item = queueSlas.toMap
    )
  }

  def setSlasUpdatesToMessage(updates: SetSlasUpdate): SetSlasUpdateMessage =
    SetSlasUpdateMessage(
      update = Option(slasUpdateToMessage(updates.update)),
      maybeOriginalEffectiveFrom = updates.maybeOriginalEffectiveFrom,
    )

  def setSlasUpdatesFromMessage(message: SetSlasUpdateMessage): SetSlasUpdate =
    SetSlasUpdate(
      update = message.update.map(slasUpdateFromMessage).getOrElse(throw new Exception("No update in message")),
      maybeOriginalEffectiveFrom = message.maybeOriginalEffectiveFrom,
    )
}
