package uk.gov.homeoffice.drt.ports

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.config.AirportConfigs

class PortRegionTest extends AnyWordSpec with Matchers {
  "Ports" should {
    "Contain all the ports we have AirportConfig for" in {
      PortRegion.ports.toList.sorted should ===(AirportConfigs.allPortConfigs.map(_.portCode).sorted)
    }
  }
  "fromPort" should {
    "Find the correct region for a port" in {
      PortRegion.fromPort(PortCode("LHR")) should ===(PortRegion.Heathrow)
    }
    "Find the correct region for a port regardless of case" in {
      PortRegion.fromPort(PortCode("lhr")) should ===(PortRegion.Heathrow)
    }
    "Throw an exception if the port is not found" in {
      an[Exception] should be thrownBy PortRegion.fromPort(PortCode("XXX"))
    }
  }
}
