package uk.gov.homeoffice.drt.actor

import org.apache.commons.csv.{CSVFormat, CSVParser}
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.ports.Terminals.Terminal

import scala.io.Source
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.{Failure, Success, Try}

object WalkTimeProvider {
  private val log = LoggerFactory.getLogger(getClass)

  def apply(maybeGatesCsvPath: Option[String], maybeStandsCsvPath: Option[String]): (Terminal, String, String) => Option[Int] = {
    val maybeGates = maybeGatesCsvPath.map(walkTimes)
    val maybeStands = maybeStandsCsvPath.map(walkTimes)

    (terminal: Terminal, gate: String, stand: String) => {
      (maybeGates, maybeStands) match {
        case (None, None) => None
        case (Some(gates), None) => gates.get((terminal, gate))
        case (None, Some(stands)) => stands.get((terminal, stand))
        case (Some(gates), Some(stands)) => stands.get((terminal, stand)).orElse(gates.get((terminal, gate)))
      }
    }
  }

  private def walkTimes(csvPath: String): Map[(Terminal, String), Int] =
    Try {
      val source = Source.fromFile(csvPath)
      val csvContent = source.getLines().mkString("\n")

      source.close()

      CSVParser
        .parse(csvContent, CSVFormat.DEFAULT.withFirstRecordAsHeader())
        .iterator().asScala.toList
        .map { row =>
          val gateOrStand = Try(row.get("gate")).getOrElse(row.get("stand"))
          val terminal = Terminal(row.get("terminal"))
          val walkTime = row.get("walktime").toInt
          ((terminal, gateOrStand), walkTime)
        }.toMap
    } match {
      case Success(walkTimes) =>
        walkTimes
      case Failure(e) =>
        throw new Exception(s"Failed to load walk times from $csvPath", e)
    }
}
