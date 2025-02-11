package uk.gov.homeoffice.drt.jsonformats

import spray.json.{DefaultJsonProtocol, JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.model.{PassengersSummaries, PassengersSummary}
import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.time.LocalDate

import scala.collection.immutable.SortedMap

object PassengersSummaryFormat extends DefaultJsonProtocol {
  implicit object JsonFormat extends RootJsonFormat[PassengersSummary] {

    implicit val dt: LocalDateJsonFormat.JsonFormat.type = LocalDateJsonFormat.JsonFormat

    override def read(json: JsValue): PassengersSummary = {
      val obj = json.asJsObject

      obj.getFields("regionName", "portCode", "totalCapacity", "drtQueueCounts", "bxQueueCounts") match {
        case Seq(JsString(regionName), JsString(portCode), JsNumber(totalCapacity), JsArray(drtQueueCounts), JsArray(bxQueueCounts)) =>
          val drtQueueCountsMap = queueCountsMap(drtQueueCounts)
          val bxQueueCountsMap = queueCountsMap(bxQueueCounts)
          val maybeTerminalName = obj.fields.get("terminalName").map(_.convertTo[String])
          val maybeDate = obj.fields.get("date").map(_.convertTo[LocalDate])
          val maybeHour = obj.fields.get("hour").map(_.convertTo[Int])
          PassengersSummary(regionName, portCode, maybeTerminalName, totalCapacity.toInt, drtQueueCountsMap, bxQueueCountsMap, maybeDate, maybeHour)
        case _ => throw new Exception("PassengersSummary expected")
      }
    }

    override def write(obj: PassengersSummary): JsValue = {
      val maybeTerminal = obj.terminalName.map(terminalName => "terminalName" -> terminalName.toJson)
      val maybeDate = obj.maybeDate.map(date => "date" -> date.toJson)
      val maybeHour = obj.maybeHour.map(hour => "hour" -> hour.toJson)

      val fields = SortedMap(
        "regionName" -> JsString(obj.regionName),
        "portCode" -> JsString(obj.portCode),
        "totalCapacity" -> JsNumber(obj.totalCapacity),
        "drtQueueCounts" -> queueCounts(obj.drtQueueCounts),
        "bxQueueCounts" -> queueCounts(obj.bxQueueCounts),
      ) ++ maybeTerminal ++ maybeDate ++ maybeHour

      JsObject(fields)
    }
  }

  private def queueCountsMap(queueCounts: Vector[JsValue]): Map[Queue, Int] =
    queueCounts.map {
      case queueCount: JsObject =>
        val queueName = queueCount.fields("queueName").convertTo[String]
        val queue = Queue(queueName)
        val count = queueCount.fields("count").convertTo[Int]
        queue -> count
    }.toMap

  private def queueCounts(queueCounts: Map[Queue, Int]): JsArray =
    JsArray(queueCounts.map {
      case (queue, count) => JsObject(Map(
        "queueName" -> JsString(queue.toString),
        "queueDisplayName" -> JsString(Queues.displayName(queue)),
        "count" -> JsNumber(count)
      ))
    }.toVector)

  implicit val passengersSummariesFormat: RootJsonFormat[PassengersSummaries] = jsonFormat1(PassengersSummaries.apply)
}
