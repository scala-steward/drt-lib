package uk.gov.homeoffice.drt.redlist

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.Nationality

class RedListUpdatesTest extends AnyWordSpec {
  "RedListUpdates" should {
    val addGbrUsa = RedListUpdate(1L, Map("GBR" -> "UK", "USA" -> "USA"), List())
    "be able to remove a date" in {
      val redListUpdates = RedListUpdates(Map(1L -> addGbrUsa))
      val redListUpdates2 = redListUpdates.remove(1L)
      assert(redListUpdates2.isEmpty)
    }
    "be able to remove a country" in {
      val redListUpdates = RedListUpdates(Map(1L -> addGbrUsa))
      val removal = RedListUpdate(2L, Map(), List("GBR"))
      val redListUpdates2 = redListUpdates ++ RedListUpdates(Map(2L -> removal))
      assert(redListUpdates2.countryCodesByName(2L) == Map("USA" -> "USA"))
      assert(redListUpdates2.redListNats(2L) == Seq(Nationality("USA")))
    }
    "be able to update a date" in {
      val redListUpdates = RedListUpdates(Map(1L -> addGbrUsa))
      val update = RedListUpdate(2L, Map("USA" -> "USA"), List())
      val redListUpdates2 = redListUpdates ++ RedListUpdates(Map(2L -> update))
      assert(redListUpdates2.countryCodesByName(2L) == Map("GBR" -> "UK", "USA" -> "USA"))
    }
    "give an empty list when given a date from when the red list was scrapped (15/12/2021)" in {
      val redListUpdates = RedListUpdates(Map(1L -> addGbrUsa))
      assert(redListUpdates.countryCodesByName(1639267200000L) == Map())
    }
  }
}
