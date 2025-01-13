package uk.gov.homeoffice.drt.jsonformats

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}
import uk.gov.homeoffice.drt.models.{DayPaxFigures, PaxFigures}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal

object DayPaxFiguresJsonFormat extends DefaultJsonProtocol {

  import uk.gov.homeoffice.drt.jsonformats.LocalDateJsonFormat._

  implicit val dayPaxFiguresJsonFormat: RootJsonFormat[DayPaxFigures] = jsonFormat9(DayPaxFigures.apply)

  implicit object TerminalJsonFormat extends RootJsonFormat[Terminal] {
    override def read(json: JsValue): Terminal = json match {
      case JsString(dateStr) => Terminal(dateStr)
    }

    override def write(obj: Terminal): JsValue = JsString(obj.toString)
  }

  implicit val paxFiguresJsonFormat: RootJsonFormat[PaxFigures] = jsonFormat3(PaxFigures.apply)
}
