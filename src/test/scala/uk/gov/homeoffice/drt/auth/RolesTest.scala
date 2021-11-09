package uk.gov.homeoffice.drt.auth

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.auth.Roles._

class RolesTest extends AnyWordSpec with Matchers {
  "The port roles list" should {
    "Contain the correct ports" in {
      Roles.portRoles should ===(Set(BFS, BHD, BHX, BRS, CWL, DSA, EDI, EMA, GLA, HUY, LBA, LCY, LGW, LHR, LPL, LTN, MAN, NCL, PIK, STN))
    }
  }
}
