package uk.gov.homeoffice.drt.auth

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.auth.Roles._

class RolesTest extends AnyWordSpec with Matchers {
  "The port roles list" should {
    "Contain the correct ports" in {
      Roles.portRoles should ===(Set(ABZ, BFS, BHD, BHX, BRS, BOH, CWL, DSA, EDI, EMA, EXT, GLA, HUY, INV, LBA, LCY, LGW, LHR, LPL, LTN, MAN, MME, NCL, NQY, NWI, PIK, SEN, SOU, STN))
    }
  }
}
