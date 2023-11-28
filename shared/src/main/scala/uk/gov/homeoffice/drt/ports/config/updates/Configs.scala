package uk.gov.homeoffice.drt.ports.config.updates

import scala.collection.immutable.SortedMap

trait Configs[A] {
  val configs: SortedMap[Long, A]

  def configForDate(at: Long): Option[A]

  def remove(effectiveFrom: Long): Configs[A]

  def update(update: ConfigUpdate[A]): Configs[A]
}
