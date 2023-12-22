package uk.gov.homeoffice.drt.db

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.queries.{PassengersHourlyQueries, PassengersHourlySerialiser}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk, FastTrack, NonEeaDesk}
import uk.gov.homeoffice.drt.ports.Terminals.{T2, T3}
import uk.gov.homeoffice.drt.time.{LocalDate, UtcDate}

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

      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(global)(terminal, paxHourlyToInsert)), 2.second)

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

      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(global)(terminal, paxHourly)), 2.second)
      val paxHourlyUpdate = List(
        PassengersHourly(portCode, terminal, FastTrack, UtcDate(2020, 1, 1), 1, 1),
        PassengersHourly(portCode, terminal, FastTrack, UtcDate(2020, 1, 1), 2, 2),
      ).map(ph => PassengersHourlySerialiser.toRow(ph, 0L))
      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(global)(terminal, paxHourlyUpdate)), 2.second)

      val expected = List(
        PassengersHourly(portCode, terminal, FastTrack, UtcDate(2020, 1, 1), 1, 1),
        PassengersHourly(portCode, terminal, FastTrack, UtcDate(2020, 1, 1), 2, 2),
        PassengersHourly(portCode, terminal, NonEeaDesk, UtcDate(2020, 1, 1), 3, 3),
      )

      val rows = db.run(PassengersHourlyQueries.get(portCode.iata, terminal.toString, UtcDate(2020, 1, 1).toISOString)).futureValue
      rows.toSet.map(ph => PassengersHourlySerialiser.fromRow(ph)) should be(expected.toSet)
    }
  }

  "PassengerHourlyQueries totalForPortAndDate" should {
    "return the total passengers for a port and local date (spanning 2 utc dates)" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val paxHourly = List(
        PassengersHourly(portCode, terminal, EeaDesk, UtcDate(2023, 6, 9), 22, 10),
        PassengersHourly(portCode, terminal, EeaDesk, UtcDate(2023, 6, 9), 23, 50),
        PassengersHourly(portCode, terminal, EGate, UtcDate(2023, 6, 10), 1, 25),
        PassengersHourly(portCode, terminal, EGate, UtcDate(2023, 6, 10), 23, 10),
      ).map(ph => PassengersHourlySerialiser.toRow(ph, 0L))

      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(global)(terminal, paxHourly)), 2.second)

      val result = db.run(PassengersHourlyQueries.totalForPortAndDate(portCode.iata, None)(global)(LocalDate(2023, 6, 10))).futureValue

      result should be(75)
    }

    "return the total passengers for a port, terminal and local date (spanning 2 utc dates)" in {
      val portCode = PortCode("LHR")
      val paxHourlyT2 = List(
        PassengersHourly(portCode, T2, EeaDesk, UtcDate(2023, 6, 9), 22, 10),
        PassengersHourly(portCode, T2, EeaDesk, UtcDate(2023, 6, 9), 23, 50),
        PassengersHourly(portCode, T2, EGate, UtcDate(2023, 6, 10), 1, 25),
        PassengersHourly(portCode, T2, EGate, UtcDate(2023, 6, 10), 23, 10),
      ).map(ph => PassengersHourlySerialiser.toRow(ph, 0L))
      val paxHourlyT3 = List(
        PassengersHourly(portCode, T3, EeaDesk, UtcDate(2023, 6, 9), 22, 10),
        PassengersHourly(portCode, T3, EeaDesk, UtcDate(2023, 6, 9), 23, 100),
        PassengersHourly(portCode, T3, EGate, UtcDate(2023, 6, 10), 1, 50),
        PassengersHourly(portCode, T3, EGate, UtcDate(2023, 6, 10), 23, 10),
      ).map(ph => PassengersHourlySerialiser.toRow(ph, 0L))

      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(global)(T2, paxHourlyT2)), 2.second)
      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(global)(T3, paxHourlyT3)), 2.second)

      val resultT2 = db.run(PassengersHourlyQueries.totalForPortAndDate(portCode.iata, Option(T2.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT2 should be(75)

      val resultT3 = db.run(PassengersHourlyQueries.totalForPortAndDate(portCode.iata, Option(T3.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT3 should be(150)
    }

    "return the hourly passengers for a port, terminal and local date (spanning 2 utc dates)" in {
      val portCode = PortCode("LHR")
      val paxHourlyT2 = List(
        PassengersHourly(portCode, T2, EeaDesk, UtcDate(2023, 6, 9), 22, 10),
        PassengersHourly(portCode, T2, EeaDesk, UtcDate(2023, 6, 9), 23, 50),
        PassengersHourly(portCode, T2, EGate, UtcDate(2023, 6, 10), 1, 25),
        PassengersHourly(portCode, T2, EGate, UtcDate(2023, 6, 10), 23, 10),
      ).map(ph => PassengersHourlySerialiser.toRow(ph, 0L))
      val paxHourlyT3 = List(
        PassengersHourly(portCode, T3, EeaDesk, UtcDate(2023, 6, 9), 22, 10),
        PassengersHourly(portCode, T3, EeaDesk, UtcDate(2023, 6, 9), 23, 100),
        PassengersHourly(portCode, T3, EGate, UtcDate(2023, 6, 10), 1, 50),
        PassengersHourly(portCode, T3, EGate, UtcDate(2023, 6, 10), 23, 10),
      ).map(ph => PassengersHourlySerialiser.toRow(ph, 0L))

      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(global)(T2, paxHourlyT2)), 2.second)
      Await.result(db.run(PassengersHourlyQueries.replaceHours(portCode)(global)(T3, paxHourlyT3)), 2.second)

      val resultT2 = db.run(PassengersHourlyQueries.hourlyForPortAndDate(portCode.iata, Option(T2.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT2 should be(Map((UtcDate(2023, 6, 9), 23) -> 50, (UtcDate(2023, 6, 10), 1) -> 25))

      val resultT3 = db.run(PassengersHourlyQueries.hourlyForPortAndDate(portCode.iata, Option(T3.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT3 should be(Map((UtcDate(2023, 6, 9), 23) -> 100, (UtcDate(2023, 6, 10), 1) -> 50))
    }
  }
}
