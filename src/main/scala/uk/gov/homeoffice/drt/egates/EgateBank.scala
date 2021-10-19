package uk.gov.homeoffice.drt.egates

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import upickle.default.{ReadWriter, macroRW}

import scala.collection.immutable.NumericRange

case class EgateBank(gates: IndexedSeq[Boolean])

object EgateBank {
  implicit val rw: ReadWriter[EgateBank] = macroRW

  def fromAirportConfig(banks: Iterable[Int]): IndexedSeq[EgateBank] = {
    banks.to[IndexedSeq].map { size => EgateBank(IndexedSeq.fill(size)(true)) }
  }
}

case class EgateBanksUpdate(effectiveFrom: Long, banks: IndexedSeq[EgateBank])

object EgateBanksUpdate {
  implicit val rw: ReadWriter[EgateBanksUpdate] = macroRW
}

case class EgateBanksUpdates(updates: List[EgateBanksUpdate]) {
  def applyForDate(atDate: Long, banks: IndexedSeq[EgateBank]): IndexedSeq[EgateBank] = {
    updatesForDate(atDate) match {
      case Some(EgateBanksUpdate(_, update)) => update
      case None => banks
    }
  }

  private def updatesForDate(atDate: Long): Option[EgateBanksUpdate] =
    updates.sortBy(_.effectiveFrom).reverse.find(_.effectiveFrom < atDate)

  def forPeriod(millis: NumericRange[Long]): IndexedSeq[Seq[EgateBank]] = {
    (updatesForDate(millis.min) ++ updates.filter(u => millis.min <= u.effectiveFrom && u.effectiveFrom <= millis.max)).toSeq
      .sortBy(_.effectiveFrom)
      .reverse
      .foldLeft(List[(NumericRange[Long], Seq[EgateBank])]()) {
        case (acc, updates) =>
          acc.map(_._1).sortBy(_.min).headOption match {
            case None => List((startMillis(millis, updates) to millis.max, updates.banks))
            case Some(upToMillis) =>
              val banksForPeriod = (startMillis(millis, updates) until upToMillis.min, updates.banks)
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

  private def startMillis(millis: NumericRange[Long], updates: EgateBanksUpdate) = {
    val from = if (updates.effectiveFrom < millis.min) millis.min else updates.effectiveFrom
    from
  }

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
