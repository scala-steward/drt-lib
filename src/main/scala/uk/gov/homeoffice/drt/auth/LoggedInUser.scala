package uk.gov.homeoffice.drt.auth

import uk.gov.homeoffice.drt.auth.Roles.{PortAccess, Role}
import upickle.default.{macroRW, ReadWriter => RW}

case class LoggedInUser(userName: String, id: String, email: String, roles: Set[Role]) {
  def hasRole(role: Role): Boolean = roles.exists(_.name == role.name)

  def canAccessPort(port: String): Boolean = Roles.parse(port) match {
    case Some(role: PortAccess) => roles.contains(role)
    case _ => false
  }

  def portRoles: Set[Role] = roles.filter(_.isInstanceOf[PortAccess])
}

object LoggedInUser {
  implicit val rw: RW[LoggedInUser] = macroRW
}

case class ShouldReload(shouldReload: Boolean)
