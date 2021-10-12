package uk.gov.homeoffice.drt.egates

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import upickle.default.{ReadWriter, macroRW}

case class EgateBank(gates: IndexedSeq[Boolean])

object EgateBank {
  implicit val rw: ReadWriter[EgateBank] = macroRW
}

case class EgateBanksUpdate(effectiveFrom: Long, banks: IndexedSeq[EgateBank])

object EgateBanksUpdate {
  implicit val rw: ReadWriter[EgateBanksUpdate] = macroRW
}

case class EgateBanksUpdates(updates: List[EgateBanksUpdate]) {
  def applyForDate(atDate: Long, banks: IndexedSeq[EgateBank]): IndexedSeq[EgateBank] = {
    updates.sortBy(_.effectiveFrom).reverse.find(_.effectiveFrom < atDate) match {
      case Some(EgateBanksUpdate(_, update)) => update
      case None => banks
    }
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
}
