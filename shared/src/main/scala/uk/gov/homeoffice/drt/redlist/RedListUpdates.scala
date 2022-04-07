package uk.gov.homeoffice.drt.redlist

import uk.gov.homeoffice.drt.Nationality
import upickle.default.{ReadWriter, macroRW}

import scala.collection.immutable.Map


case class RedListUpdate(effectiveFrom: Long, additions: Map[String, String], removals: List[String])

object RedListUpdate {
  implicit val rw: ReadWriter[RedListUpdate] = macroRW
}

case class RedListUpdates(updates: Map[Long, RedListUpdate]) {
  lazy val isEmpty: Boolean = updates.isEmpty

  def remove(effectiveFrom: Long): RedListUpdates = copy(updates = updates.filterKeys(_ != effectiveFrom).view.toMap)

  def update(setRedListUpdate: SetRedListUpdate): RedListUpdates =
    copy(updates = updates
      .filter {
        case (effectiveFrom, update) => effectiveFrom != setRedListUpdate.originalDate
      } + (setRedListUpdate.redListUpdate.effectiveFrom -> setRedListUpdate.redListUpdate)
    )

  def ++(other: RedListUpdates): RedListUpdates = copy(updates = updates ++ other.updates)

  def countryCodesByName(date: Long): Map[String, String] =
    updates
      .filterKeys(changeDate => changeDate <= date)
      .toList.sortBy(_._1)
      .foldLeft(Map[String, String]()) {
        case (acc, (date, updates)) => (acc ++ updates.additions) -- updates.removals
      }

  def redListNats(date: Long): Iterable[Nationality] =
    countryCodesByName(date).values.map(Nationality(_))
}

object RedListUpdates {
  val empty: RedListUpdates = RedListUpdates(Map())

  implicit val rw: ReadWriter[RedListUpdates] = macroRW
}

