package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.UniqueArrival
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.serialisers.FlightRowHelper.generateFlight
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.time.{SDate, UtcDate}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class FlightDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  private val db = TestDatabase.db

  import TestDatabase.profile.api._

  private val portCode: PortCode = PortCode("LHR")

  val dao: FlightDao = FlightDao()

  SchemaUtils.printStatements(dao.table.schema.createStatements)

  before {
    Await.result(
      db.run(DBIO.seq(
        dao.table.schema.dropIfExists,
        dao.table.schema.createIfNotExists)
      ), 2.second)
  }

  "insertOrUpdate" should {
    "insert records into an empty table" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))

      Await.result(db.run(dao.insertOrUpdate(portCode)(flight)), 2.second)

      val rows = Await.result(db.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 123)), 1.second)
      rows should be(Option(flight))
    }

    "replace existing queues are replaced with new records for same time periods" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))
      val flight2 = generateFlight(123, 1L, PortCode("JFK")).copy(apiFlight = flight.apiFlight.copy(Stand = Option("updated stand")))

      Await.result(db.run(dao.insertOrUpdate(portCode)(flight)), 2.second)
      Await.result(db.run(dao.insertOrUpdate(portCode)(flight2)), 2.second)

      val rows = Await.result(db.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 123)), 1.second)
      rows should be(Option(flight2))
    }
  }

  "remove" should {
    "remove records from the table" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))

      Await.result(db.run(dao.insertOrUpdate(portCode)(flight)), 2.second)
      Await.result(db.run(dao.remove(portCode)(UniqueArrival(123, T1, 1L, PortCode("JFK")))), 2.second)

      val rows = Await.result(db.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 123)), 1.second)
      rows should be(None)
    }
  }

  "removeMulti" should {
    "remove multiple records from the table" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))
      val flight2 = generateFlight(124, 1L, PortCode("JFK"))

      Await.result(db.run(dao.insertOrUpdate(portCode)(flight)), 2.second)
      Await.result(db.run(dao.insertOrUpdate(portCode)(flight2)), 2.second)
      Await.result(
        db.run(dao.removeMulti(portCode)(Seq(
          UniqueArrival(123, T1, 1L, PortCode("JFK")),
          UniqueArrival(124, T1, 1L, PortCode("JFK"))))),
        2.second)

      val rows = Await.result(db.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 123)), 1.second)
      rows should be(None)
    }

    "remove only the records requested and leave other records alone" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))
      val flight2 = generateFlight(124, 1L, PortCode("JFK"))

      Await.result(db.run(dao.insertOrUpdate(portCode)(flight)), 2.second)
      Await.result(db.run(dao.insertOrUpdate(portCode)(flight2)), 2.second)
      Await.result(
        db.run(dao.removeMulti(portCode)(Seq(UniqueArrival(123, T1, 1L, PortCode("JFK"))))),
        2.second)

      val rows = Await.result(db.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 123)), 1.second)
      rows should be(None)
      val rows2 = Await.result(db.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 124)), 1.second)
      rows2 should be(Option(flight2))
    }
  }

  "removeAllBefore" should {
    "remove all records before the given date" in {
      Seq(
        generateFlight(125, SDate("2024-11-11").millisSinceEpoch, PortCode("JFK")),
        generateFlight(125, SDate("2024-11-12").millisSinceEpoch, PortCode("JFK")),
        generateFlight(125, SDate("2024-11-13").millisSinceEpoch, PortCode("JFK")),
      ).map(flight => Await.result(db.run(dao.insertOrUpdate(portCode)(flight)), 2.second))

      Await.result(db.run(dao.removeAllBefore(UtcDate(2024, 11,13))), 2.second)

      Seq(
        (SDate("2024-11-11").millisSinceEpoch, None),
        (SDate("2024-11-12").millisSinceEpoch, None),
        (SDate("2024-11-13").millisSinceEpoch, Option(generateFlight(125, SDate("2024-11-13").millisSinceEpoch, PortCode("JFK")))),
      ).map {
        case (minute, expected) =>
          val rows = Await.result(db.run(dao.get(portCode)(PortCode("JFK"), T1, minute, 125)), 1.second)
          rows should be(expected)
      }
    }
  }
}
