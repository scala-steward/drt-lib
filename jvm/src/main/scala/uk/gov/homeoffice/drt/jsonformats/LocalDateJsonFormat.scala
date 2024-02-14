package uk.gov.homeoffice.drt.jsonformats

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}
import uk.gov.homeoffice.drt.time.LocalDate

object LocalDateJsonFormat extends DefaultJsonProtocol {
  implicit object JsonFormat extends RootJsonFormat[LocalDate] {
    override def read(json: JsValue): LocalDate = json match {
      case JsString(dateStr) => LocalDate.parse(dateStr).getOrElse(throw new IllegalArgumentException(s"Could not parse date from $dateStr"))
    }

    override def write(obj: LocalDate): JsValue = JsString(obj.toISOString)
  }
}
