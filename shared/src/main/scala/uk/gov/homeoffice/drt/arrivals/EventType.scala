package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.arrivals.EventTypes.{CI, DC, InvalidEventType}
import uk.gov.homeoffice.drt.ports.ClassNameForToString
import upickle.default
import upickle.default.{ReadWriter, macroRW}

sealed trait EventType extends ClassNameForToString {
  val name: String
}

object EventType {
  implicit val rw: default.ReadWriter[EventType] = ReadWriter.merge(macroRW[DC.type], macroRW[CI.type], macroRW[InvalidEventType.type])

  def apply(eventType: String): EventType = eventType match {
    case "DC" => DC
    case "CI" => CI
    case _ => InvalidEventType
  }
}

object EventTypes {

  case object DC extends EventType {
    override val name: String = "DC"
  }

  case object CI extends EventType {
    override val name: String = "CI"
  }

  object InvalidEventType extends EventType {
    override val name: String = "Invalid"
  }

}
