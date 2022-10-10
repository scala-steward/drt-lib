package uk.gov.homeoffice.drt.auth

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.AppEnvironment.{OtherEnv, PreProdEnv, ProdEnv, TestEnv}
import uk.gov.homeoffice.drt.auth.Roles._

class LoggedInUserSpec extends AnyWordSpec with Matchers {

  "LoggedInUsers" should {

    "have StaffMovements Role" in {

      val roleName = Roles.parse("staff-movements:edit")

      val loggedInUser = LoggedInUser("test", "testId", "test@drt.com", Set(roleName).flatten)

      roleName mustBe Some(StaffMovementsEdit)

      loggedInUser.hasRole(StaffMovementsEdit) mustBe true
    }

    "have ArrivalSimulationUpload Role" in {

      val roleName = Roles.parse("arrival-simulation-upload")

      val loggedInUser = LoggedInUser("test", "testId", "test@drt.com", Set(roleName).flatten)

      roleName mustBe Some(ArrivalSimulationUpload)

      loggedInUser.hasRole(ArrivalSimulationUpload) mustBe true
    }

    "have StaffMovementsExport Role" in {

      val roleName = Roles.parse("staff-movements:export")

      val loggedInUser = LoggedInUser("test", "testId", "test@drt.com", Set(roleName).flatten)

      roleName mustBe Some(StaffMovementsExport)

      loggedInUser.hasRole(StaffMovementsExport) mustBe true
    }

    "not have StaffMovements Role if role name doesn't exist" in {

      val roleName = Roles.parse("unknown")

      val staffRole = Roles.parse("staff:edit")

      val loggedInUser = LoggedInUser("test", "testId", "test@drt.com", Set(roleName, staffRole).flatten)

      roleName mustBe None

      loggedInUser.hasRole(StaffMovementsEdit) mustBe false

      loggedInUser.hasRole(StaffEdit) mustBe true

    }

    "have faq view Role" in {

      val roleName = Roles.parse("faq:view")

      val loggedInUser = LoggedInUser("test", "testId", "test@drt.com", Set(roleName).flatten)

      roleName mustBe Some(FaqView)

      loggedInUser.hasRole(FaqView) mustBe true
    }

    "provide list of port access roles" in {
      val user = LoggedInUser("", "", "", Set(LHR, STN, StaffMovementsExport))
      val portRoles = user.portRoles

      portRoles mustEqual Set(LHR, STN)
    }

    "return false for a non-accessible port" in {
      val user = LoggedInUser("", "", "", Set(LHR, STN, StaffMovementsExport))
      val isAccessible = user.canAccessPort(BHX.toString)

      isAccessible mustEqual false
    }

    "return true for an accessible port" in {
      val user = LoggedInUser("", "", "", Set(LHR, STN, StaffMovementsExport))
      val isAccessible = user.canAccessPort(LHR.toString)

      isAccessible mustEqual true
    }

    "know if the user is not restricted to particular environments" in {
      val user = LoggedInUser("", "", "", Set())
      user.canAccessEnvironment(ProdEnv) mustEqual true
      user.canAccessEnvironment(PreProdEnv) mustEqual true
      user.canAccessEnvironment(TestEnv) mustEqual true
      user.canAccessEnvironment(OtherEnv) mustEqual true
    }

    "know if the user is restricted to the prod environment" in {
      val user = LoggedInUser("", "", "", Set(AccessOnlyProd))
      user.canAccessEnvironment(ProdEnv) mustEqual true
      user.canAccessEnvironment(PreProdEnv) mustEqual false
      user.canAccessEnvironment(TestEnv) mustEqual false
      user.canAccessEnvironment(OtherEnv) mustEqual false
    }

    "know if the user is restricted to the preprod environment" in {
      val user = LoggedInUser("", "", "", Set(AccessOnlyPreprod))
      user.canAccessEnvironment(ProdEnv) mustEqual false
      user.canAccessEnvironment(PreProdEnv) mustEqual true
      user.canAccessEnvironment(TestEnv) mustEqual false
      user.canAccessEnvironment(OtherEnv) mustEqual false
    }

    "know if the user is restricted to the prod & preprod environments" in {
      val user = LoggedInUser("", "", "", Set(AccessOnlyProd, AccessOnlyPreprod))
      user.canAccessEnvironment(ProdEnv) mustEqual true
      user.canAccessEnvironment(PreProdEnv) mustEqual true
      user.canAccessEnvironment(TestEnv) mustEqual false
      user.canAccessEnvironment(OtherEnv) mustEqual false
    }
  }
}

