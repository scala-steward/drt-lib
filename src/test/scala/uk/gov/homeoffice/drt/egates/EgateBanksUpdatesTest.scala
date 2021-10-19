package uk.gov.homeoffice.drt.egates

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EgateBanksUpdatesTest extends AnyWordSpec with Matchers {
  "An set of updates" should {
    "provide banks for a given time period" in {
      val banks1 = IndexedSeq(EgateBank(IndexedSeq(true, true, true)))
      val banks2 = IndexedSeq(EgateBank(IndexedSeq(true, true, false)))
      val update1 = EgateBanksUpdate(0L, banks1)
      val update2 = EgateBanksUpdate(10L, banks2)
      val updates = EgateBanksUpdates(List(update1, update2))

      updates.forPeriod(8L to 11L) should ===(IndexedSeq(banks1, banks1, banks2, banks2))
    }
  }
}
