package drt.auth

import upickle.default.{macroRW, ReadWriter => RW}

case class LoggedInUser(userName: String, id: String, email: String, roles: Set[Role]) {
  def hasRole(role: Role): Boolean = roles.exists(_.name == role.name)
}

object LoggedInUser {
  implicit val rw: RW[LoggedInUser] = macroRW
}

case class ShouldReload(shouldReload: Boolean)
