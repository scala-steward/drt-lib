package drt.auth

import org.scalatest.{MustMatchers, WordSpec}

class LoggedInUserSpec extends WordSpec with MustMatchers {

  "LoggedInUsers" should {

    "have StaffMovement Role" in {

      val roleName: Role = Roles.parse("staff-movement:edit").get

      val loggedInUser = LoggedInUser("test", "testId", "test@drt.com", Set(roleName))

      roleName mustBe StaffMovementsEdit

      loggedInUser.hasRole(StaffMovementsEdit) mustBe true
    }

    "not have StaffMovement Role if role name not exists" in {

      val roleName = Roles.parse("unknown")

      val staffRole = Roles.parse("staff:edit")

      val loggedInUser = LoggedInUser("test", "testId", "test@drt.com", Set(roleName, staffRole).flatten)

      roleName mustBe None

      loggedInUser.hasRole(StaffMovementsEdit) mustBe false

      loggedInUser.hasRole(StaffEdit) mustBe true

    }
  }
}

