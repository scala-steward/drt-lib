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
  }
}

