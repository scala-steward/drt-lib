package uk.gov.homeoffice.drt.services

import uk.gov.homeoffice.drt.ports.{AirportInfo, PortCode}
import uk.gov.homeoffice.drt.redlist.RedListUpdates
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch

import scala.io.Codec
import scala.util.Try


object AirportInfoService {

  lazy val airportInfoByIataPortCode: Map[String, AirportInfo] = {
    val bufferedSource = scala.io.Source.fromURL(getClass.getResource("/airports.dat"))(Codec.UTF8)
    bufferedSource.getLines().map { l =>
      Try {
        val splitRow: Array[String] = l.split(",")
        val sq: String => String = stripQuotes
        AirportInfo(sq(splitRow(1)), sq(splitRow(2)), sq(splitRow(3)), sq(splitRow(4)))
      }.getOrElse(
        AirportInfo("Unknown", "Unknown", "Unknown", "Unknown")
      )
    }.map(ai => (ai.code, ai)).toMap
  }

  def airportInfo(code: PortCode): Option[AirportInfo] = airportInfoByIataPortCode.get(code.iata)

  def stripQuotes(row1: String): String = {
    row1.substring(1, row1.length - 1)
  }

  def isRedListed(portToCheck: PortCode, forDate: MillisSinceEpoch, redListUpdates: RedListUpdates): Boolean = airportInfoByIataPortCode
    .get(portToCheck.iata)
    .exists(ai => redListUpdates.countryCodesByName(forDate).contains(ai.country))
}
