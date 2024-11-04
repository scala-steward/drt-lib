package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import specs2.arguments.sequential
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.serialisers.FlightRowHelper.generateFlight
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class FlightDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  sequential

  private val db = TestDatabase.db

  import TestDatabase.profile.api._

  val dao: FlightDao = FlightDao(PortCode("LHR"))

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

      Await.result(db.run(dao.insertOrUpdate(flight)), 2.second)

      val rows = Await.result(db.run(dao.get("LHR", "JFK", "T1", 1L, 123)), 1.second)
      rows should be(Seq(flight))
    }

    "replace existing queues are replaced with new records for same time periods" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))
      val flight2 = generateFlight(123, 1L, PortCode("JFK")).copy(apiFlight = flight.apiFlight.copy(Stand = Option("updated stand")))

      Await.result(db.run(dao.insertOrUpdate(flight)), 2.second)
      Await.result(db.run(dao.insertOrUpdate(flight2)), 2.second)

      val rows = Await.result(db.run(dao.get("LHR", "JFK", "T1", 1L, 123)), 1.second)
      rows should be(Seq(flight2))
    }
  }
}
