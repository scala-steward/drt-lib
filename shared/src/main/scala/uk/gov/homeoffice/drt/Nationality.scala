package uk.gov.homeoffice.drt

import upickle.default.{ReadWriter, macroRW}

case class Nationality(code: String) {
  override def toString: String = code
}

object Nationality {
  implicit val rw: ReadWriter[Nationality] = macroRW
}
