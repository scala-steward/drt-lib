package uk.gov.homeoffice.drt.actor.serialisation

import uk.gov.homeoffice.drt.actor.ConfigActor.SetUpdate
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.{SlaConfigs, SlasUpdate}
import uk.gov.homeoffice.drt.ports.config.updates.{ConfigUpdate, Configs}
import uk.gov.homeoffice.drt.protobuf.messages.SlasUpdates._

import scala.collection.immutable.SortedMap

object SlasMessageConversion {
  private def slasUpdateToMessage(slasUpdate: ConfigUpdate[Map[Queue, Int]]): SlasUpdateMessage =
    SlasUpdateMessage(
      effectiveFrom = Option(slasUpdate.effectiveFrom),
      queueSlas = slasUpdate.configItem.map {
        case (queue, slas) => SlasMessage(Option(queue.toString), Option(slas))
      }.toSeq,
      maybeOriginalEffectiveFrom = slasUpdate.maybeOriginalEffectiveFrom,
    )

  private def slasUpdateFromMessage[A](message: SlasUpdateMessage): ConfigUpdate[A] =
    SlasUpdate(
      effectiveFrom = message.effectiveFrom.getOrElse(throw new Exception("No effectiveFrom in message")),
      configItem = queueSlasFromMessage(message.queueSlas),
      maybeOriginalEffectiveFrom = message.maybeOriginalEffectiveFrom,
    ).asInstanceOf[ConfigUpdate[A]]

  private def queueSlasFromMessage(messages: Seq[SlasMessage]): Map[Queue, Int] =
    messages.map { slasMessage =>
      val queue = slasMessage.queue.getOrElse(throw new Exception("No queue in message"))
      val slaMinutes = slasMessage.minutes.getOrElse(throw new Exception("No sla minutes in message"))
      (Queue(queue), slaMinutes)
    }.toMap

  private def slasConfigToMessage(effectiveFrom: Long, config: Map[Queue, Int]): SlasConfigMessage =
    SlasConfigMessage(
      effectiveFrom = Option(effectiveFrom),
      queueSlas = config.map {
        case (queue, slas) => SlasMessage(Option(queue.toString), Option(slas))
      }.toSeq,
    )

  private def slasConfigFromMessage(message: SlasConfigMessage): (Long, Map[Queue, Int]) = {
    val effectiveFrom = message.effectiveFrom.getOrElse(throw new Exception("No effectiveFrom in message"))
    val configItem = queueSlasFromMessage(message.queueSlas)

    (effectiveFrom, configItem)
  }

  def setSlasUpdatesToMessage(updates: SetUpdate[Map[Queue, Int]]): SetSlasUpdateMessage =
    SetSlasUpdateMessage(
      update = Option(slasUpdateToMessage(updates.update)),
    )

  def setSlasUpdatesFromMessage[A](message: SetSlasUpdateMessage): SetUpdate[A] =
    SetUpdate(
      update = message.update.map(slasUpdateFromMessage[A]).getOrElse(throw new Exception("No update in message")),
    )

  def slaConfigsToMessage(configs: Configs[Map[Queue, Int]]): SlaConfigsMessage = SlaConfigsMessage(
    configs.configs.map {
      case (effectiveFrom, config) => slasConfigToMessage(effectiveFrom, config)
    }.toSeq
  )

  def slaConfigsFromMessage(configs: SlaConfigsMessage): SlaConfigs = SlaConfigs(
    configs = SortedMap(configs.config.map { slasConfigMessage =>
      slasConfigFromMessage(slasConfigMessage)
    }: _*)
  )
}
