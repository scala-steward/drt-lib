package uk.gov.homeoffice.drt.db

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.queries.PassengersHourlyQueries
import uk.gov.homeoffice.drt.db.serialisers.PassengersHourlySerialiser
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk, FastTrack, NonEeaDesk}
import uk.gov.homeoffice.drt.ports.Terminals.{T2, T3, Terminal}
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, UtcDate}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class PassengersHourlyQueriesTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  private val db = TestDatabase.db

  import TestDatabase.profile.api._

  before {
    Await.result(
      db.run(DBIO.seq(
        PassengersHourlyQueries.table.schema.dropIfExists,
        PassengersHourlyQueries.table.schema.createIfNotExists)
      ), 2.second)
  }

  "PassengersHourlyQueries replaceHours" should {
    "insert records into an empty table" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val paxHourly = List(
        PassengersHourly(portCode, terminal, EeaDesk, UtcDate(2020, 1, 1), 1, 1),
        PassengersHourly(portCode, terminal, EGate, UtcDate(2020, 1, 1), 2, 2),
        PassengersHourly(portCode, terminal, NonEeaDesk, UtcDate(2020, 1, 1), 3, 3),
      )
      val paxHourlyToInsert = paxHourly.map(ph => PassengersHourlySerialiser.toRow(ph, 0L))

      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(terminal, paxHourlyToInsert)), 2.second)

      val rows = db.run(PassengersHourlyQueries.get(portCode.iata, terminal.toString, UtcDate(2020, 1, 1).toISOString)).futureValue
      rows.toSet.map(PassengersHourlySerialiser.fromRow) should be(paxHourly.toSet)
    }

    "replace existing queues are replaced with new records for same time periods" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val paxHourly = List(
        PassengersHourly(portCode, terminal, EeaDesk, UtcDate(2020, 1, 1), 1, 1),
        PassengersHourly(portCode, terminal, EGate, UtcDate(2020, 1, 1), 1, 1),
        PassengersHourly(portCode, terminal, NonEeaDesk, UtcDate(2020, 1, 1), 3, 3),
      ).map(ph => PassengersHourlySerialiser.toRow(ph, 0L))

      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(terminal, paxHourly)), 2.second)
      val paxHourlyUpdate = List(
        PassengersHourly(portCode, terminal, FastTrack, UtcDate(2020, 1, 1), 1, 1),
        PassengersHourly(portCode, terminal, FastTrack, UtcDate(2020, 1, 1), 2, 2),
      ).map(ph => PassengersHourlySerialiser.toRow(ph, 0L))
      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(terminal, paxHourlyUpdate)), 2.second)

      val expected = List(
        PassengersHourly(portCode, terminal, FastTrack, UtcDate(2020, 1, 1), 1, 1),
        PassengersHourly(portCode, terminal, FastTrack, UtcDate(2020, 1, 1), 2, 2),
        PassengersHourly(portCode, terminal, NonEeaDesk, UtcDate(2020, 1, 1), 3, 3),
      )

      val rows = db.run(PassengersHourlyQueries.get(portCode.iata, terminal.toString, UtcDate(2020, 1, 1).toISOString)).futureValue
      rows.toSet.map(ph => PassengersHourlySerialiser.fromRow(ph)) should be(expected.toSet)
    }
  }

  private val portCode: PortCode = PortCode("LHR")

  private def hourlyPax(terminal: Terminal, eeaPax: Int, egatePax: Int): List[PassengersHourly] = List(
    PassengersHourly(portCode, terminal, EeaDesk, UtcDate(2023, 6, 9), 22, 10),
    PassengersHourly(portCode, terminal, EeaDesk, UtcDate(2023, 6, 9), 23, eeaPax),
    PassengersHourly(portCode, terminal, EGate, UtcDate(2023, 6, 10), 1, egatePax),
    PassengersHourly(portCode, terminal, EGate, UtcDate(2023, 6, 10), 23, 10),
  )

  private def insertHourlyPax(terminal: Terminal, eeaPax: Int, egatePax: Int): Unit = {
    val paxHourly = hourlyPax(terminal, eeaPax, egatePax).map(ph => PassengersHourlySerialiser.toRow(ph, 0L))
    Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(terminal, paxHourly)), 2.second)
  }

  "PassengerHourlyQueries totalForPortAndDate" should {
    "return the total passengers for a port and local date (spanning 2 utc dates)" in {
      insertHourlyPax(T2, 50, 25)

      val result = db.run(PassengersHourlyQueries.totalForPortAndDate(portCode.iata, None)(global)(LocalDate(2023, 6, 10))).futureValue

      result should be(75)
    }

    "return the total passengers for a port, terminal and local date (spanning 2 utc dates)" in {
      val portCode = PortCode("LHR")
      insertHourlyPax(T2, 50, 25)
      insertHourlyPax(T3, 50, 25)

      val resultT2 = db.run(PassengersHourlyQueries.totalForPortAndDate(portCode.iata, Option(T2.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT2 should be(75)

      val resultT3 = db.run(PassengersHourlyQueries.totalForPortAndDate(portCode.iata, Option(T3.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT3 should be(75)
    }

    "return the hourly passengers for a port, terminal and local date (spanning 2 utc dates)" in {
      val portCode = PortCode("LHR")
      insertHourlyPax(T2, 50, 25)
      insertHourlyPax(T3, 100, 50)

      val resultT2 = db.run(PassengersHourlyQueries.hourlyForPortAndDate(portCode.iata, Option(T2.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT2 should be(Map(SDate(2023, 6, 9, 23, 0).millisSinceEpoch -> 50, SDate(2023, 6, 10, 1, 0).millisSinceEpoch -> 25))

      val resultT3 = db.run(PassengersHourlyQueries.hourlyForPortAndDate(portCode.iata, Option(T3.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT3 should be(Map(SDate(2023, 6, 9, 23, 0).millisSinceEpoch -> 100, SDate(2023, 6, 10, 1, 0).millisSinceEpoch -> 50))
    }
  }

  "PassengerHourlyQueries queueTotalsForPortAndDate" should {
    "return the total passengers for a port and local date (spanning 2 utc dates)" in {
      insertHourlyPax(T2, 50, 25)

      val result = db.run(PassengersHourlyQueries.queueTotalsForPortAndDate(portCode.iata, None)(global)(LocalDate(2023, 6, 10))).futureValue

      result should be(Map(EeaDesk -> 50, EGate -> 25))
    }

    "return the total passengers for a port, terminal and local date (spanning 2 utc dates)" in {
      val portCode = PortCode("LHR")
      insertHourlyPax(T2, 50, 25)
      insertHourlyPax(T3, 100, 50)

      val resultT2 = db.run(PassengersHourlyQueries.queueTotalsForPortAndDate(portCode.iata, Option(T2.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT2 should be(Map(EeaDesk -> 50, EGate -> 25))

      val resultT3 = db.run(PassengersHourlyQueries.queueTotalsForPortAndDate(portCode.iata, Option(T3.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT3 should be(Map(EeaDesk -> 100, EGate -> 50))
    }
  }
}
