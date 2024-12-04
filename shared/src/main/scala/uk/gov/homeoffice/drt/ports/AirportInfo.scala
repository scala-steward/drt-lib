package uk.gov.homeoffice.drt.ports

import upickle.default._

case class AirportInfo(airportName: String, city: String, country: String, code: String)

object AirportInfo {
  implicit val rw: ReadWriter[AirportInfo] = macroRW
}
