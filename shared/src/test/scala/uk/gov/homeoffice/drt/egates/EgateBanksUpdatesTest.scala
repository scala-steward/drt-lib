package uk.gov.homeoffice.drt.egates

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EgateBanksUpdatesTest extends AnyWordSpec with Matchers {
  "A set of updates" should {
    val banks1 = IndexedSeq(EgateBank(IndexedSeq(true, true, true)))
    val banks2 = IndexedSeq(EgateBank(IndexedSeq(true, true, false)))
    val update1 = EgateBanksUpdate(0L, banks1)
    val update2 = EgateBanksUpdate(10L, banks2)

    "provide banks for a given time period that reflect the updates" in {
      val updates = EgateBanksUpdates(List(update1, update2))

      updates.forPeriod(8L to 11L) should ===(IndexedSeq(banks1, banks1, banks2, banks2))
    }

    "provide banks for a given time period which matches the time range step" in {
      val updates = EgateBanksUpdates(List(update1, update2))
      val timeRangeWithStep10 = 0L to 100L by 10
      val timeRangeLength = timeRangeWithStep10.length

      updates.forPeriod(timeRangeWithStep10).length should ===(timeRangeLength)
    }

    "provide empty banks for time with no prior updates" in {
      val updates = EgateBanksUpdates(List())
      val timeRangeWithStep10 = 0L to 100L by 10
      val timeRangeLength = timeRangeWithStep10.length

      updates.forPeriod(timeRangeWithStep10).length should ===(timeRangeLength)
    }
  }
}
