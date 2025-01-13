package uk.gov.homeoffice.drt.ports.config

import org.scalatest.wordspec.AnyWordSpec

class AirportConfigsTest extends AnyWordSpec {
  "AirportConfigs" should {
    "have allPorts" in {
      assert(AirportConfigs.allPorts.nonEmpty)
    }
    "have allPortConfigs" in {
      assert(AirportConfigs.allPortConfigs.nonEmpty)
    }
    "have portGroups" in {
      assert(AirportConfigs.portGroups.nonEmpty)
    }
    "have confByPort" in {
      assert(AirportConfigs.confByPort.nonEmpty)
    }
  }
}
