package uk.gov.homeoffice.drt.ports.config.slas

import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.updates.{ConfigUpdate, Configs}

import scala.collection.immutable.SortedMap


case class SlaConfigs(configs: SortedMap[Long, Map[Queue, Int]]) extends Configs[Map[Queue, Int]] {
  override def configForDate(at: Long): Option[Map[Queue, Int]] = {
    configs.toSeq.findLast { case (effectiveFrom, _) => effectiveFrom < at }.map(_._2)
  }

  override def remove(effectiveFrom: Long): SlaConfigs =
    copy(configs = configs - effectiveFrom)

  override def update(update: ConfigUpdate[Map[Queue, Int]]): SlaConfigs =
    update.maybeOriginalEffectiveFrom match {
      case Some(originalEffectiveFrom) =>
        val withoutRemoved = remove(originalEffectiveFrom)
        withoutRemoved.copy(configs = withoutRemoved.configs + (update.effectiveFrom -> update.configItem))
      case None =>
        copy(configs = configs + (update.effectiveFrom -> update.configItem))
    }
}

object SlaConfigs {
  val empty: SlaConfigs = SlaConfigs(SortedMap())
}



