package uk.gov.homeoffice.drt.actor.serialisation

import uk.gov.homeoffice.drt.actor.ConfigActor.SetUpdate
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.{Slas, SlasUpdate}
import uk.gov.homeoffice.drt.ports.config.updates.{ConfigUpdate, UpdatesWithHistory}
import uk.gov.homeoffice.drt.protobuf.messages.SlasUpdates._

import scala.collection.immutable.SortedMap

object SlasMessageConversion {
  private def slasUpdateToMessage(slasUpdate: ConfigUpdate[Map[Queue, Int]]): SlasUpdateMessage =
    SlasUpdateMessage(
      effectiveFrom = Option(slasUpdate.effectiveFrom),
      queueSlas = slasUpdate.item.map {
        case (queue, slas) => SlasMessage(Option(queue.toString), Option(slas))
      }.toSeq
    )

  private def slasUpdateFromMessage[A](message: SlasUpdateMessage): ConfigUpdate[A] = {
    val queueSlas = message.queueSlas.map { slasMessage =>
      val queue = slasMessage.queue.getOrElse(throw new Exception("No queue in message"))
      val slaMinutes = slasMessage.minutes.getOrElse(throw new Exception("No sla minutes in message"))
      (Queue(queue), slaMinutes)
    }

    SlasUpdate(
      effectiveFrom = message.effectiveFrom.getOrElse(throw new Exception("No effectiveFrom in message")),
      item = queueSlas.toMap
    ).asInstanceOf[ConfigUpdate[A]]
  }

  def setSlasUpdatesToMessage(updates: SetUpdate[Map[Queue, Int]]): SetSlasUpdateMessage =
    SetSlasUpdateMessage(
      update = Option(slasUpdateToMessage(updates.update)),
      maybeOriginalEffectiveFrom = updates.maybeOriginalEffectiveFrom,
    )

  def setSlasUpdatesFromMessage[A](message: SetSlasUpdateMessage): SetUpdate[A] =
    SetUpdate(
      update = message.update.map(slasUpdateFromMessage[A]).getOrElse(throw new Exception("No update in message")),
      maybeOriginalEffectiveFrom = message.maybeOriginalEffectiveFrom,
    )

  def slasUpdatesToMessage(updates: UpdatesWithHistory[Map[Queue, Int]]): SlaUpdatesMessage = SlaUpdatesMessage(
    updates = updates.updates.map {
      case (effectiveFrom, item) => slasUpdateToMessage(SlasUpdate(effectiveFrom, item))
    }.toSeq
  )

  def slasUpdatesFromMessage(updates: SlaUpdatesMessage): Slas = Slas(
    updates = SortedMap(updates.updates.map { msg =>
      val update = slasUpdateFromMessage(msg)
      (update.effectiveFrom, update.item)
    }: _*)
  )
}
