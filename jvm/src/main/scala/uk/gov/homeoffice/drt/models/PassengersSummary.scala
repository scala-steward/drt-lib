package uk.gov.homeoffice.drt.models

import spray.json.{DefaultJsonProtocol, JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}
import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.time.LocalDate

import scala.collection.immutable.SortedMap


case class PassengersSummary(regionName: String,
                          portCode: String,
                          terminalName: Option[String],
                          totalPcpPax: Int,
                          queueCounts: Map[Queue, Int],
                          maybeDate: Option[LocalDate],
                          maybeHour: Option[Int],
                         )

object PassengersSummaryFormat extends DefaultJsonProtocol {
  implicit object JsonFormat extends RootJsonFormat[PassengersSummary] {

    override def read(json: JsValue): PassengersSummary = throw new Exception("Not implemented")

    override def write(obj: PassengersSummary): JsValue = {
      val maybeTerminal = obj.terminalName.map(terminalName => "terminalName" -> JsString(terminalName))
      val maybeDate = obj.maybeDate.map(date => "date" -> JsString(date.toISOString))
      val maybeHour = obj.maybeHour.map(hour => "hour" -> JsNumber(hour))

      val fields = SortedMap(
        "regionName" -> JsString(obj.regionName),
        "portCode" -> JsString(obj.portCode),
        "totalPcpPax" -> JsNumber(obj.totalPcpPax),
        "queueCounts" -> JsArray(obj.queueCounts.map {
          case (queue, count) => JsObject(Map(
            "queueName" -> JsString(Queues.displayName(queue)),
            "count" -> JsNumber(count)
          ))
        }.toVector),
      ) ++ maybeTerminal ++ maybeDate ++ maybeHour

      JsObject(fields)
    }
  }
}
