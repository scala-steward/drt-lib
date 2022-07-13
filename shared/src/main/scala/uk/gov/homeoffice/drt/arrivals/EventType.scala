package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.arrivals.EventTypes.{CI, DC, InvalidEventType}
import uk.gov.homeoffice.drt.ports.ClassNameForToString
import upickle.default
import upickle.default.{ReadWriter, macroRW}

sealed trait EventType extends ClassNameForToString

object EventType {
  implicit val rw: default.ReadWriter[EventType] = ReadWriter.merge(macroRW[DC.type], macroRW[CI.type], macroRW[InvalidEventType.type])

  def apply(eventType: String): EventType = eventType match {
    case "DC" => DC
    case "CI" => CI
    case _ => InvalidEventType
  }
}

object EventTypes {

  case object DC extends EventType

  case object CI extends EventType

  object InvalidEventType extends EventType

}
