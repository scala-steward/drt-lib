package uk.gov.homeoffice.drt.ports.config.updates

import scala.collection.immutable.SortedMap

trait UpdatesWithHistory[A] {
  val updates: SortedMap[Long, A]

  def updatesForDate(at: Long): Option[A]

  def remove(effectiveFrom: Long): UpdatesWithHistory[A]

  def update(newEffectiveFrom: Long, item: A): UpdatesWithHistory[A]

  def update(newEffectiveFrom: Long, item: A, originalEffectiveFrom: Long): UpdatesWithHistory[A]
}
