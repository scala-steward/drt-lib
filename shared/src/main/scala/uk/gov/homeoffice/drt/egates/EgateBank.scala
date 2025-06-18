package uk.gov.homeoffice.drt.egates

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import upickle.default.{ReadWriter, macroRW}

import scala.collection.immutable.NumericRange

sealed trait WorkloadProcessor {
  val maxCapacity: Int
  val openCount: Int
  val isClosed: Boolean
  val isFullyOpen: Boolean
}

case object Desk extends WorkloadProcessor {
  override val maxCapacity: Int = 1
  override val openCount: Int = 1
  override val isClosed = false
  override val isFullyOpen: Boolean = true
}

case class EgateBank(gates: IndexedSeq[Boolean]) extends WorkloadProcessor {
  override lazy val maxCapacity: Int = gates.size
  override lazy val openCount: Int = gates.count(identity)
  override lazy val isClosed: Boolean = openCount == 0
  override lazy val isFullyOpen: Boolean = openCount == maxCapacity
}

object EgateBank {
  implicit val rw: ReadWriter[EgateBank] = macroRW

  def fromAirportConfig(banks: Iterable[Int]): IndexedSeq[EgateBank] = {
    banks.map { size => EgateBank(IndexedSeq.fill(size)(true)) }.toIndexedSeq
  }
}

case class EgateBanksUpdate(effectiveFrom: Long, banks: IndexedSeq[EgateBank])

object EgateBanksUpdate {
  implicit val rw: ReadWriter[EgateBanksUpdate] = macroRW
}

case class EgateBanksUpdates(updates: List[EgateBanksUpdate]) {
  def updatesForDate(atDate: Long): Option[EgateBanksUpdate] =
    updates.sortBy(_.effectiveFrom).findLast(_.effectiveFrom < atDate)

  def forPeriod(millis: NumericRange[Long]): IndexedSeq[Seq[EgateBank]] = {
    updatesForDate(millis.min) ++ updates.filter(u => millis.min <= u.effectiveFrom && u.effectiveFrom <= millis.max) match {
      case noUpdates if noUpdates.isEmpty =>
        IndexedSeq.fill(millis.length)(Seq())

      case applicableUpdates =>
        applicableUpdates.toSeq
          .sortBy(_.effectiveFrom)
          .reverse
          .foldLeft(List[(NumericRange[Long], Seq[EgateBank])]()) {
            case (acc, updates) =>
              acc.map(_._1).sortBy(_.min).headOption match {
                case None =>
                  val range = startMillis(millis, updates) to millis.max by millis.step
                  List((range, updates.banks))
                case Some(upToMillis) =>
                  val range = startMillis(millis, updates) until upToMillis.min by millis.step
                  val banksForPeriod = (range, updates.banks)
                  banksForPeriod :: acc
              }
          }
          .flatMap {
            case (range, banks) => range.map(m => (m, banks))
          }
          .sortBy(_._1)
          .map(_._2)
          .toIndexedSeq
    }
  }

  def forTime(millis: Long): Seq[EgateBank] = {
    updatesForDate(millis) match {
      case None => Seq()
      case Some(applicableUpdate) => applicableUpdate.banks
    }
  }

  private def startMillis(millis: NumericRange[Long], updates: EgateBanksUpdate): Long =
    if (updates.effectiveFrom < millis.min) millis.min else updates.effectiveFrom

  def update(setEgateBanksUpdate: SetEgateBanksUpdate): EgateBanksUpdates = {
    val updated: List[EgateBanksUpdate] = updates
      .filter { update =>
        update.effectiveFrom != setEgateBanksUpdate.originalDate
      } :+ setEgateBanksUpdate.egateBanksUpdate

    copy(updates = updated)
  }

  def remove(effectiveFrom: Long): EgateBanksUpdates = copy(updates = updates.filter(_.effectiveFrom != effectiveFrom))
}

object EgateBanksUpdates {
  val empty: EgateBanksUpdates = EgateBanksUpdates(List())

  implicit val rw: ReadWriter[EgateBanksUpdates] = macroRW
}

case class PortEgateBanksUpdates(updatesByTerminal: Map[Terminal, EgateBanksUpdates]) {
  def update(update: SetEgateBanksUpdate): PortEgateBanksUpdates = {
    val updatedTerminal = updatesByTerminal.getOrElse(update.terminal, EgateBanksUpdates.empty).update(update)
    copy(updatesByTerminal.updated(update.terminal, updatedTerminal))
  }

  def remove(delete: DeleteEgateBanksUpdates): PortEgateBanksUpdates = {
    val updatedTerminal = updatesByTerminal.getOrElse(delete.terminal, EgateBanksUpdates.empty).remove(delete.millis)
    copy(updatesByTerminal.updated(delete.terminal, updatedTerminal))
  }

  def size: Int = updatesByTerminal.map(_._2.updates.length).sum
}

object PortEgateBanksUpdates {
  implicit val rw: ReadWriter[PortEgateBanksUpdates] = macroRW
}
