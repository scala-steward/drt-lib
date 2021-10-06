package uk.gov.homeoffice.drt.egates

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

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
//case class EgateBankIntraDayUpdate(startMinute: Int, durationMinutes: Int)
//case class RecurringWeekly(startDate: Long, endDate: Long, daysOfWeek: Set[Int])

class EgateBankTest extends AnyWordSpec with Matchers {
  val banks = IndexedSeq(
    EgateBank(IndexedSeq(true, true, true, true, true)),
    EgateBank(IndexedSeq(true, true, true, true, true)),
  )

  val updateAt10: EgateBanksUpdate = EgateBanksUpdate(10L, IndexedSeq(
    EgateBank(IndexedSeq(true, true, true, true, true)),
    EgateBank(IndexedSeq(true, true, true, false, true)),
  ))

  val updateAt20: EgateBanksUpdate = EgateBanksUpdate(20L, IndexedSeq(
    EgateBank(IndexedSeq(true, true, true, true, true)),
    EgateBank(IndexedSeq(false, true, true, false, true)),
  ))

  "An egates bank" should {
    "reflect the most recent update at a given time" in {
      val updates = EgateBanksUpdates(List(updateAt10))

      val banksAt11 = updates.applyForDate(11L, banks)

      banksAt11 should ===(updateAt10.banks)
    }
    "remain the same given no previous updates" in {
      val updates = EgateBanksUpdates(List(updateAt10))

      val banksAt9 = updates.applyForDate(9L, banks)

      banksAt9 should ===(banks)
    }
    "only reflect the most recent update, and not upcoming updates" in {
      val updates = EgateBanksUpdates(List(updateAt10, updateAt20))

      val banksAt15 = updates.applyForDate(15L, banks)

      banksAt15 should ===(updateAt10.banks)
    }
  }
}
