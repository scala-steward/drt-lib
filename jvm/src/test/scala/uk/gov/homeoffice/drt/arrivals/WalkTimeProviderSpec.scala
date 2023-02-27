package uk.gov.homeoffice.drt.arrivals

import org.apache.commons.csv.{CSVFormat, CSVParser}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2, Terminal}

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Try


case class WalkTimeProvider(walkTimes: Map[(Terminal, String), Int]) {
  def walkTime(arrival: Arrival): Option[Int] = {
    val terminal = arrival.Terminal
    val standOrGate = arrival.Stand.getOrElse(arrival.Gate.getOrElse(""))
    walkTimes.get((terminal, standOrGate))
  }
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

class WalkTimeProviderSpec extends AnyWordSpec with Matchers {
  "A walk time provider" should {
    "read a gates csv" in {
      val walkTimeProvider = WalkTimeProvider(getClass.getClassLoader.getResource("gate-walk-times.csv").getPath)
      walkTimeProvider.walkTimes should===(Map[(Terminal, String), Int]((T1, "A1") -> 120))
    }
    "read a stands csv" in {
      val walkTimeProvider = WalkTimeProvider(getClass.getClassLoader.getResource("stand-walk-times.csv").getPath)
      walkTimeProvider.walkTimes should===(Map[(Terminal, String), Int]((T2, "A1a") -> 180))
    }
  }
}
