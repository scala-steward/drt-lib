package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.{PaxTypesAndQueues, Queues}
import uk.gov.homeoffice.drt.splits.ApiSplitsToSplitRatio

object ArrivalExportHeadings {
  private val arrivalHeadings: String = Seq(
    "IATA",
    "ICAO",
    "Origin",
    "Gate/Stand",
    "Status",
    "Scheduled",
    "Predicted Arrival",
    "Est Arrival",
    "Act Arrival",
    "Est Chox",
    "Act Chox",
    "Minutes off scheduled",
    "Est PCP",
    "Total Pax",
    "PCP Pax",
    "Invalid API"
  ).mkString(",")

  private val splitsHeadings: String = Seq("API", "Historical", "Terminal Average").map(headingsForSplitSource).mkString(",")

  private val apiAdditionalHeadings: String = Seq("Nationalities", "Ages").mkString(",")

  private val regionalExportPrefixHeadings: String = Seq("Region", "Port", "Terminal").mkString(",")

  private def headingsForSplitSource(source: String): String = queueNamesInOrder
    .map(q => s"$source ${Queues.displayName(q)}")
    .mkString(",")

  val queueNamesInOrder: Seq[Queue] = ApiSplitsToSplitRatio.queuesFromPaxTypeAndQueue(PaxTypesAndQueues.inOrder)

  val actualApiHeadings: String = PaxTypesAndQueues.inOrder.map(heading => s"API Actual - ${heading.displayName}").mkString(",")

  val arrivalWithSplitsHeadings: String = Seq(arrivalHeadings, splitsHeadings).mkString(",")

  val arrivalWithSplitsAndRawApiHeadings: String = Seq(arrivalWithSplitsHeadings, actualApiHeadings, apiAdditionalHeadings).mkString(",")

  val regionalExportHeadings: String = Seq(regionalExportPrefixHeadings, arrivalWithSplitsAndRawApiHeadings).mkString(",")
}
