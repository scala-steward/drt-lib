package uk.gov.homeoffice.drt.egates

case class EgateBank(gates: IndexedSeq[Boolean])

case class EgateBanksUpdate(effectiveFrom: Long, banks: IndexedSeq[EgateBank])

case class EgateBanksUpdates(updates: List[EgateBanksUpdate]) {
  def applyForDate(atDate: Long, banks: IndexedSeq[EgateBank]): IndexedSeq[EgateBank] = {
    updates.sortBy(_.effectiveFrom).reverse.find(_.effectiveFrom < atDate) match {
      case Some(EgateBanksUpdate(effectiveFrom, update)) => update
      case None => banks
    }
  }
}

case class SetEgateBanksUpdate(originalDate: Long, update: EgateBanksUpdate)
