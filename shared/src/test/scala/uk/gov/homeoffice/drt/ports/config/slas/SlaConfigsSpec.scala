package uk.gov.homeoffice.drt.ports.config.slas

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.Queues.EeaDesk

import scala.collection.immutable.SortedMap

class SlaConfigsSpec extends AnyWordSpec {
  "SlaConfigs" should {
    "include a new config when updating, given a no original effective from date" in {
      val configs = SlaConfigs.empty
      val update = SlasUpdate(1L, Map(), None)
      val updated = configs.update(update)
      assert(updated.configs == Map(update.effectiveFrom -> update.configItem))
    }
    "replace an existing config when updating, given a no original effective from date" in {
      val configs = SlaConfigs(SortedMap(1L -> Map(EeaDesk -> 25)))
      val update = SlasUpdate(1L, Map(EeaDesk -> 60), None)
      val updated = configs.update(update)
      assert(updated.configs == Map(update.effectiveFrom -> update.configItem))
    }
    "replace an existing config when updating, given an original effective from date" in {
      val configs = SlaConfigs(SortedMap(1L -> Map(EeaDesk -> 25)))
      val update = SlasUpdate(2L, Map(EeaDesk -> 60), Option(1L))
      val updated = configs.update(update)
      assert(updated.configs == Map(update.effectiveFrom -> update.configItem))
    }
    "remove an existing config" in {
      val configs = SlaConfigs(SortedMap(1L -> Map(EeaDesk -> 25)))
      val updated = configs.remove(1L)
      assert(updated.configs == Map())
    }
    "return the last applicable config for a given date" in {
      val configs = SlaConfigs(SortedMap(
        150L -> Map(EeaDesk -> 25),
        200L -> Map(EeaDesk -> 60),
      ))
      assert(configs.configForDate(150L) == Option(Map(EeaDesk -> 25)))
      assert(configs.configForDate(200L) == Option(Map(EeaDesk -> 60)))
    }
    "return none when the given date has no prior config" in {
      val configs = SlaConfigs(SortedMap(
        100L -> Map(EeaDesk -> 25),
        200L -> Map(EeaDesk -> 60),
      ))
      assert(configs.configForDate(50L).isEmpty)
    }
  }
}
