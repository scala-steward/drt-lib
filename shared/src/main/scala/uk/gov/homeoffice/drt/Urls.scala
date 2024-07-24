package uk.gov.homeoffice.drt

import uk.gov.homeoffice.drt.auth.Roles
import uk.gov.homeoffice.drt.auth.Roles.PortAccess

case class Urls(rootDomain: String, useHttps: Boolean) {
  val protocol: String = if (useHttps) "https://" else "http://"

  val rootUrl = s"$protocol$rootDomain"

  def portCodeFromUrl(lhrUrl: String): Option[String] = {
    val maybeDomain = lhrUrl.split("://").reverse.headOption
    val maybePortCodeString = maybeDomain.flatMap(_.toUpperCase.split("\\.").toList.headOption)
    maybePortCodeString.flatMap(Roles.parse).collect {
      case pa: PortAccess => pa.name
    }
  }

  def logoutUrlForPort(port: String): String = {
    val portUrl = urlForPort(port)
    s"$portUrl/oauth2/sign_out?redirect=$portUrl"
  }

  def urlForPort(port: String): String = s"$protocol${port.toLowerCase}.$rootDomain"
}
