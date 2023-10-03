package uk.gov.homeoffice.drt.ports.config.slas

import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.updates.{ConfigUpdate, UpdatesWithHistory}

import scala.collection.immutable.SortedMap


case class SlasUpdate(effectiveFrom: Long, item: Map[Queue, Int]) extends ConfigUpdate[Map[Queue, Int]]

object SlasUpdate {
  implicit val rw: upickle.default.ReadWriter[SlasUpdate] = upickle.default.macroRW
}

case class SlaUpdates(updates: SortedMap[Long, Map[Queue, Int]]) extends UpdatesWithHistory[Map[Queue, Int]] {
  override def updatesForDate(at: Long): Option[Map[Queue, Int]] = {
    updates.toSeq.findLast { case (effectiveFrom, _) => effectiveFrom < at }.map(_._2)
  }

  override def update(newEffectiveFrom: Long, incomingUpdate: Map[Queue, Int]): SlaUpdates =
    copy(updates = updates.updated(newEffectiveFrom, incomingUpdate))

  override def remove(effectiveFrom: Long): SlaUpdates =
    copy(updates = updates - effectiveFrom)

  override def update(newEffectiveFrom: Long, item: Map[Queue, Int], originalEffectiveFrom: Long): SlaUpdates =
    remove(originalEffectiveFrom).update(newEffectiveFrom, item)
}
