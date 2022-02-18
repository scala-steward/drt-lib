package uk.gov.homeoffice.drt.ports

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.config.AirportConfigs

class PortRegionTest extends AnyWordSpec with Matchers {
  "All ports" should {
    "Contain all the ports we have AirportConfig for" in {
      PortRegion.ports.toList.sorted should ===(AirportConfigs.allPortConfigs.map(_.portCode).toList.sorted)
    }
  }
}
