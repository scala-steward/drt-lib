package uk.gov.homeoffice.drt.jsonformats

import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.time.LocalDate

object LocalDateJsonFormat extends DefaultJsonProtocol {
  implicit object JsonFormat extends RootJsonFormat[LocalDate] {
    override def read(json: JsValue): LocalDate = json match {
      case JsObject(fields) =>
        val year = fields("year").convertTo[Int]
        val month = fields("month").convertTo[Int]
        val day = fields("day").convertTo[Int]
        LocalDate(year, month, day)
    }

    override def write(obj: LocalDate): JsValue = {
      JsObject(
        "year" -> obj.year.toJson,
        "month" -> obj.month.toJson,
        "day" -> obj.day.toJson,
      )
    }
  }
}
