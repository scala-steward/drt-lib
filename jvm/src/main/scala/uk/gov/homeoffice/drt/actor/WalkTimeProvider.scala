package uk.gov.homeoffice.drt.actor

import org.apache.commons.csv.{CSVFormat, CSVParser}
import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.ports.Terminals.Terminal

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Try

case class WalkTimeProvider(walkTimes: Map[(Terminal, String), Int]) {
  def walkTime(terminal: Terminal, gateOrStand: String): Option[Int] =
    walkTimes.get((terminal, gateOrStand))
}

object WalkTimeProvider {
  def apply(csvPath: String): WalkTimeProvider = {
    val source = scala.io.Source.fromFile(csvPath)
    val csvContent = source.getLines().mkString("\n")
    source.close()
    val csv = CSVParser.parse(csvContent, CSVFormat.DEFAULT.withFirstRecordAsHeader())
    val rows = csv.iterator().asScala.toList

    val times: Map[(Terminal, String), Int] = rows.map { row =>
      val gateOrStand = Try(row.get("gate")).getOrElse(row.get("stand"))
      val terminal = Terminal(row.get("terminal"))
      val walkTime = row.get("walktime").toInt
      ((terminal, gateOrStand), walkTime)
    }.toMap

    WalkTimeProvider(times)
  }
}
