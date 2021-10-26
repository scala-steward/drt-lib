package uk.gov.homeoffice.drt.egates

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


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

      val banksAt11 = updates.forPeriod(11L to 11L)

      banksAt11 should ===(Iterable(updateAt10.banks))
    }
    "be empty given no previous updates" in {
      val updates = EgateBanksUpdates(List(updateAt10))

      val banksAt9 = updates.forPeriod(9L to 9L)

      banksAt9 should ===(Iterable(Seq()))
    }
    "only reflect the most recent update, and not upcoming updates" in {
      val updates = EgateBanksUpdates(List(updateAt10, updateAt20))

      val banksAt15 = updates.forPeriod(15L to 15L)

      banksAt15 should ===(Iterable(updateAt10.banks))
    }
    "have capacity if it contains at least one open gate" in {
      EgateBank(IndexedSeq(true, false)).hasCapacity should ===(true)
    }
    "not have capacity if it contains no open gate" in {
      EgateBank(IndexedSeq(false, false)).hasCapacity should ===(false)
    }
  }
}
