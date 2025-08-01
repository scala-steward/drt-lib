package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.serialisers.BorderCrossingSerialiser
import uk.gov.homeoffice.drt.db.tables.GateTypes.{EGate, Pcp}
import uk.gov.homeoffice.drt.db.tables._
import uk.gov.homeoffice.drt.ports.Queues.QueueDesk
import uk.gov.homeoffice.drt.ports.{PortCode, Queues}
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2, T3, Terminal}
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, UtcDate}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class BorderCrossingDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  private val dao: BorderCrossingDao.type = BorderCrossingDao

  import TestDatabase.profile.api._

  before {
    Await.result(
      TestDatabase.run(DBIO.seq(
        dao.table.schema.dropIfExists,
        dao.table.schema.createIfNotExists)
      ), 2.second)
  }

  def insert(portCode: PortCode): (Terminal, GateType, Iterable[BorderCrossingRow]) => DBIOAction[Int, NoStream, Effect.Write] =
    dao.replaceHours(portCode)

  "replaceHours" should {
    "insert records into an empty table" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val paxHourly = List(
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), EGate, 1, 1),
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), EGate, 2, 2),
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), EGate, 3, 3),
      )
      val paxHourlyToInsert = paxHourly.map(ph => BorderCrossingSerialiser.toRow(ph, 0L))

      Await.result(TestDatabase.run(insert(portCode)(terminal, EGate, paxHourlyToInsert)), 2.second)

      val rows = TestDatabase.run(dao.get(portCode.iata, terminal.toString, UtcDate(2020, 1, 1).toISOString)).futureValue
      rows.toSet.map(BorderCrossingSerialiser.fromRow) should be(paxHourly.toSet)
    }

    "replace existing queues are replaced with new records for same time periods" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val paxHourly = List(
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), Pcp, 3, 3),
      ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))
      Await.result(TestDatabase.run(insert(portCode)(terminal, Pcp, paxHourly)), 2.second)

      val paxHourlyUpdate = List(
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), Pcp, 2, 2),
      ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))
      Await.result(TestDatabase.run(insert(portCode)(terminal, Pcp, paxHourlyUpdate)), 2.second)

      val expected = List(
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), Pcp, 2, 2),
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), Pcp, 3, 3),
      )

      val rows = TestDatabase.run(dao.get(portCode.iata, terminal.toString, UtcDate(2020, 1, 1).toISOString)).futureValue
      rows.toSet.map(ph => BorderCrossingSerialiser.fromRow(ph)) should be(expected.toSet)
    }

    "only insert/replace entries for the port and terminal specified" in {
      val portCode = PortCode("LHR")
      val otherPortCode = PortCode("JFK")
      val terminal = T2
      val otherTerminal = T1
      val paxHourly = List(
        BorderCrossing(otherPortCode, otherTerminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(otherPortCode, otherTerminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(otherPortCode, otherTerminal, UtcDate(2020, 1, 1), Pcp, 3, 3),
      ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))

      Await.result(TestDatabase.run(insert(portCode)(terminal, Pcp, paxHourly)), 2.second)
      val rows = TestDatabase.run(dao.get(otherPortCode.iata, otherTerminal.toString, UtcDate(2020, 1, 1).toISOString)).futureValue
      rows.toSet.map(ph => BorderCrossingSerialiser.fromRow(ph)) should be(Set())
    }

    "only insert/replace entries for the port specified" in {
      val portCode = PortCode("LHR")
      val otherPortCode = PortCode("JFK")
      val terminal = T2
      val paxHourly = List(
        BorderCrossing(otherPortCode, terminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(otherPortCode, terminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(otherPortCode, terminal, UtcDate(2020, 1, 1), Pcp, 3, 3),
      ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))

      Await.result(TestDatabase.run(insert(portCode)(terminal, Pcp, paxHourly)), 2.second)
      val rows = TestDatabase.run(dao.get(otherPortCode.iata, terminal.toString, UtcDate(2020, 1, 1).toISOString)).futureValue
      rows.toSet.map(ph => BorderCrossingSerialiser.fromRow(ph)) should be(Set())
    }

    "only insert/replace entries for the terminal specified" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val otherTerminal = T1
      val paxHourly = List(
        BorderCrossing(portCode, otherTerminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(portCode, otherTerminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(portCode, otherTerminal, UtcDate(2020, 1, 1), Pcp, 3, 3),
      ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))

      Await.result(TestDatabase.run(insert(portCode)(terminal, Pcp, paxHourly)), 2.second)
      val rows = TestDatabase.run(dao.get(portCode.iata, otherTerminal.toString, UtcDate(2020, 1, 1).toISOString)).futureValue
      rows.toSet.map(ph => BorderCrossingSerialiser.fromRow(ph)) should be(Set())
    }
  }

  private val portCode: PortCode = PortCode("LHR")

  private def insertHourlyPax(terminal: Terminal, deskPax: Int, egatePax: Int, date: LocalDate): Unit = {
    val sdate = SDate(date)
    val sDate = sdate.getLocalNextMidnight.addMinutes(-1)
    val utcDate = sDate.toUtcDate
    val utcDayBefore = sDate.addDays(-1).toUtcDate
    val paxHourly = List(
      BorderCrossing(portCode, terminal, utcDayBefore, EGate, 22, 10),
      BorderCrossing(portCode, terminal, utcDayBefore, Pcp, 23, deskPax),
      BorderCrossing(portCode, terminal, utcDate, EGate, 1, egatePax),
      BorderCrossing(portCode, terminal, utcDate, Pcp, 23, 10),
    ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))
    Await.result(TestDatabase.run(insert(portCode)(terminal, Pcp, paxHourly)), 2.second)
    Await.result(TestDatabase.run(insert(portCode)(terminal, EGate, paxHourly)), 2.second)
  }

  "totalForPortAndDate" should {
    "return the total passengers for a port and local date (spanning 2 utc dates)" in {
      insertHourlyPax(T2, 50, 25, LocalDate(2023, 6, 10))

      val result = TestDatabase.run(dao.totalForPortAndDate(portCode.iata, None)(global)(LocalDate(2023, 6, 10))).futureValue

      result should be(75)
    }

    "return the total passengers for a port, terminal and local date (spanning 2 utc dates)" in {
      val portCode = PortCode("LHR")
      insertHourlyPax(T2, 50, 25, LocalDate(2023, 6, 10))
      insertHourlyPax(T3, 50, 25, LocalDate(2023, 6, 10))

      val resultT2 = TestDatabase.run(dao.totalForPortAndDate(portCode.iata, Option(T2.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT2 should be(75)

      val resultT3 = TestDatabase.run(dao.totalForPortAndDate(portCode.iata, Option(T3.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT3 should be(75)
    }

    "return the hourly passengers for a port, terminal and local date (spanning 2 utc dates)" in {
      val portCode = PortCode("LHR")
      insertHourlyPax(T2, 50, 25, LocalDate(2023, 6, 10))
      insertHourlyPax(T3, 100, 50, LocalDate(2023, 6, 10))

      val resultT2: Map[Long, Map[Queues.Queue, Int]] = TestDatabase.run(dao.hourlyForPortAndDate(portCode.iata, Option(T2.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT2 should be(Map(
        SDate(2023, 6, 9, 23, 0).millisSinceEpoch -> Map(QueueDesk -> 50),
        SDate(2023, 6, 10, 1, 0).millisSinceEpoch -> Map(Queues.EGate -> 25),
      ))

      val resultT3 = TestDatabase.run(dao.hourlyForPortAndDate(portCode.iata, Option(T3.toString))(global)(LocalDate(2023, 6, 10))).futureValue

      resultT3 should be(Map(
        SDate(2023, 6, 9, 23, 0).millisSinceEpoch -> Map(QueueDesk -> 100),
        SDate(2023, 6, 10, 1, 0).millisSinceEpoch -> Map(Queues.EGate -> 50),
      ))
    }
  }

  "removeAllBefore" should {
    "only remove rows with a date earlier than the data specified" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val paxHourly = List(
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), Pcp, 1, 1),
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 2), Pcp, 2, 2),
        BorderCrossing(portCode, terminal, UtcDate(2020, 1, 3), Pcp, 3, 3),
      ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))
      Await.result(TestDatabase.run(insert(portCode)(terminal, Pcp, paxHourly)), 2.second)

      Seq(
        (1, Seq(BorderCrossing(portCode, terminal, UtcDate(2020, 1, 1), Pcp, 1, 1))),
        (2, Seq(BorderCrossing(portCode, terminal, UtcDate(2020, 1, 2), Pcp, 2, 2))),
        (3, Seq(BorderCrossing(portCode, terminal, UtcDate(2020, 1, 3), Pcp, 3, 3))),
      ).map {
        case (date, expected) =>
          val rows = TestDatabase.run(dao.get(portCode.iata, terminal.toString, UtcDate(2020, 1, date).toISOString)).futureValue
          rows.toSet.map(BorderCrossingSerialiser.fromRow) should be(expected.toSet)
      }

      val date = UtcDate(2020, 1, 3)
      Await.result(TestDatabase.run(dao.removeAllBefore(date)), 2.second)

      Seq(
        (1, Seq.empty),
        (2, Seq.empty),
        (3, Seq(BorderCrossing(portCode, terminal, UtcDate(2020, 1, 3), Pcp, 3, 3))),
      ).map {
        case (date, expected) =>
          val rows = TestDatabase.run(dao.get(portCode.iata, terminal.toString, UtcDate(2020, 1, date).toISOString)).futureValue
          rows.toSet.map(BorderCrossingSerialiser.fromRow) should be(expected.toSet)
      }
    }
  }

  "filterDate" should {
    val portCode = PortCode("LHR")
    val terminal = T2

    "filter rows by utc date" in {
      val paxHourly = Seq(
        BorderCrossing(portCode, terminal, UtcDate(2025, 5, 1), Pcp, 1, 1),
        BorderCrossing(portCode, terminal, UtcDate(2025, 5, 2), Pcp, 2, 2),
        BorderCrossing(portCode, terminal, UtcDate(2025, 5, 3), Pcp, 3, 3),
      ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))
      Await.result(TestDatabase.run(insert(portCode)(terminal, Pcp, paxHourly)), 2.second)

      val rows = TestDatabase.run(dao.get(portCode.iata, terminal.toString, UtcDate(2025, 5, 2).toISOString)).futureValue
      rows.toSet.map(BorderCrossingSerialiser.fromRow) should be(Set(BorderCrossing(portCode, terminal, UtcDate(2025, 5, 2), Pcp, 2, 2)))
    }
    "filter rows by local date - 23:00 to 23:00 in utc" in {
      val paxHourly = Seq(
        BorderCrossing(portCode, terminal, UtcDate(2025, 5, 1), Pcp, 22, 1),
        BorderCrossing(portCode, terminal, UtcDate(2025, 5, 1), Pcp, 23, 2),
        BorderCrossing(portCode, terminal, UtcDate(2025, 5, 2), Pcp, 0, 3),
        BorderCrossing(portCode, terminal, UtcDate(2025, 5, 2), Pcp, 1, 4),
      ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))

      val result = BorderCrossingDao.filterDate(paxHourly, LocalDate(2025, 5, 2))
        .map(BorderCrossingSerialiser.fromRow)

      result should ===(Seq(
        BorderCrossing(portCode, terminal, UtcDate(2025, 5, 1), Pcp, 23, 2),
        BorderCrossing(portCode, terminal, UtcDate(2025, 5, 2), Pcp, 0, 3),
        BorderCrossing(portCode, terminal, UtcDate(2025, 5, 2), Pcp, 1, 4)
      ))
    }
  }

  "queueTotalsForPortAndDate" should {
    "return the total pax for a port and local date that has pax spanning 2 utc dates" in {
      (SDate("2025-05-01T20:00").millisSinceEpoch to SDate("2025-05-03T20:00").millisSinceEpoch by 1.hour.toMillis).map { millis =>
        val sdate = SDate(millis)
        val date = sdate.toUtcDate
        val hour = sdate.getHours
        val paxHourly = List(
          BorderCrossing(portCode, T1, date, EGate, hour, 1),
          BorderCrossing(portCode, T1, date, Pcp, hour, 2),
        ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))
        Await.result(TestDatabase.run(insert(portCode)(T1, EGate, paxHourly)), 2.second)
        Await.result(TestDatabase.run(insert(portCode)(T1, Pcp, paxHourly)), 2.second)
      }

      val result = TestDatabase.run(dao.queueTotalsForPortAndDate(portCode.iata, None)(global)(LocalDate(2025, 5, 2))).futureValue

      result should be(Map(
        Queues.EGate -> 24,
        QueueDesk -> 48
      ))
    }
    "return the total pax for a port and utc date" in {
      (SDate("2025-05-01T20:00").millisSinceEpoch to SDate("2025-05-03T20:00").millisSinceEpoch by 1.hour.toMillis).map { millis =>
        val sdate = SDate(millis)
        val date = sdate.toUtcDate
        val hour = sdate.getHours
        val paxHourly = List(
          BorderCrossing(portCode, T1, date, EGate, hour, 1),
          BorderCrossing(portCode, T1, date, Pcp, hour, 2),
        ).map(ph => BorderCrossingSerialiser.toRow(ph, 0L))
        Await.result(TestDatabase.run(insert(portCode)(T1, EGate, paxHourly)), 2.second)
        Await.result(TestDatabase.run(insert(portCode)(T1, Pcp, paxHourly)), 2.second)
      }

      val result = TestDatabase.run(dao.queueTotalsForPortAndDate(portCode.iata, None)(global)(UtcDate(2025, 5, 2))).futureValue

      result should be(Map(
        Queues.EGate -> 24,
        QueueDesk -> 48
      ))
    }
  }
}
