package drt.auth

import org.scalatest.{MustMatchers, WordSpec}

class LoggedInUserSpec extends WordSpec with MustMatchers {

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

    "not have StaffMovements Role if role name not exists" in {

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
  }
}

