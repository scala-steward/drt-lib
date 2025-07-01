package uk.gov.homeoffice.drt.db.dao

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.{ApiFlightWithSplits, Arrival, ArrivalGenerator, UniqueArrival}
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.serialisers.FlightRowHelper.generateFlight
import uk.gov.homeoffice.drt.ports.{LiveFeedSource, PortCode}
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.time.{DateLike, LocalDate, SDate, UtcDate}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class FlightDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  import TestDatabase.profile.api._

  private val portCode: PortCode = PortCode("LHR")

  val dao: FlightDao = FlightDao()

  SchemaUtils.printStatements(dao.table.schema.createStatements)

  before {
    Await.result(
      TestDatabase.run(DBIO.seq(
        dao.table.schema.dropIfExists,
        dao.table.schema.createIfNotExists)
      ), 2.second)
  }

  "insertOrUpdate" should {
    "insert records into an empty table" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))

      Await.result(TestDatabase.run(dao.insertOrUpdate(portCode)(flight)), 2.second)

      val rows = Await.result(TestDatabase.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 123)), 1.second)
      rows should be(Option(flight))
    }

    "replace existing queues are replaced with new records for same time periods" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))
      val flight2 = generateFlight(123, 1L, PortCode("JFK")).copy(apiFlight = flight.apiFlight.copy(Stand = Option("updated stand")))

      Await.result(TestDatabase.run(dao.insertOrUpdate(portCode)(flight)), 2.second)
      Await.result(TestDatabase.run(dao.insertOrUpdate(portCode)(flight2)), 2.second)

      val rows = Await.result(TestDatabase.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 123)), 1.second)
      rows should be(Option(flight2))
    }
  }

  "remove" should {
    "remove records from the table" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))

      Await.result(TestDatabase.run(dao.insertOrUpdate(portCode)(flight)), 2.second)
      Await.result(TestDatabase.run(dao.remove(portCode)(UniqueArrival(123, T1, 1L, PortCode("JFK")))), 2.second)

      val rows = Await.result(TestDatabase.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 123)), 1.second)
      rows should be(None)
    }
  }

  "removeMulti" should {
    "remove multiple records from the table" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))
      val flight2 = generateFlight(124, 1L, PortCode("JFK"))

      Await.result(TestDatabase.run(dao.insertOrUpdate(portCode)(flight)), 2.second)
      Await.result(TestDatabase.run(dao.insertOrUpdate(portCode)(flight2)), 2.second)
      Await.result(
        TestDatabase.run(dao.removeMulti(portCode)(Seq(
          UniqueArrival(123, T1, 1L, PortCode("JFK")),
          UniqueArrival(124, T1, 1L, PortCode("JFK"))))),
        2.second)

      val rows = Await.result(TestDatabase.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 123)), 1.second)
      rows should be(None)
    }

    "remove only the records requested and leave other records alone" in {
      val flight = generateFlight(123, 1L, PortCode("JFK"))
      val flight2 = generateFlight(124, 1L, PortCode("JFK"))

      Await.result(TestDatabase.run(dao.insertOrUpdate(portCode)(flight)), 2.second)
      Await.result(TestDatabase.run(dao.insertOrUpdate(portCode)(flight2)), 2.second)
      Await.result(
        TestDatabase.run(dao.removeMulti(portCode)(Seq(UniqueArrival(123, T1, 1L, PortCode("JFK"))))),
        2.second)

      val rows = Await.result(TestDatabase.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 123)), 1.second)
      rows should be(None)
      val rows2 = Await.result(TestDatabase.run(dao.get(portCode)(PortCode("JFK"), T1, 1L, 124)), 1.second)
      rows2 should be(Option(flight2))
    }
  }

  "removeAllBefore" should {
    "remove all records before the given date" in {
      Seq(
        generateFlight(125, SDate("2024-11-11").millisSinceEpoch, PortCode("JFK")),
        generateFlight(125, SDate("2024-11-12").millisSinceEpoch, PortCode("JFK")),
        generateFlight(125, SDate("2024-11-13").millisSinceEpoch, PortCode("JFK")),
      ).map(flight => Await.result(TestDatabase.run(dao.insertOrUpdate(portCode)(flight)), 2.second))

      Await.result(TestDatabase.run(dao.removeAllBefore(UtcDate(2024, 11,13))), 2.second)

      Seq(
        (SDate("2024-11-11").millisSinceEpoch, None),
        (SDate("2024-11-12").millisSinceEpoch, None),
        (SDate("2024-11-13").millisSinceEpoch, Option(generateFlight(125, SDate("2024-11-13").millisSinceEpoch, PortCode("JFK")))),
      ).map {
        case (minute, expected) =>
          val rows = Await.result(TestDatabase.run(dao.get(portCode)(PortCode("JFK"), T1, minute, 125)), 1.second)
          rows should be(expected)
      }
    }
  }

  "flightsForPcpDateRange with LocalDates" should {
    implicit val system: ActorSystem = ActorSystem("FlightDaoTest")
    implicit val mat: Materializer = Materializer.matFromSystem
    "return flights at the beginning of a LocalDate in BST" in {
      checkScheduledIsReturnedForDate("2024-06-10T23:00:00Z", LocalDate(2024, 6, 11))
    }
    "return flights at the end of a LocalDate in BST" in {
      checkScheduledIsReturnedForDate("2024-06-11T22:45:00Z", LocalDate(2024, 6, 11))
    }
    "not return flights earlier than the beginning of a LocalDate in BST" in {
      checkScheduledIsNotReturnedForDate("2024-06-10T22:00:00Z", LocalDate(2024, 6, 11))
    }
    "not return flights later than the end of a LocalDate in BST" in {
      checkScheduledIsNotReturnedForDate("2024-06-11T23:00:00Z", LocalDate(2024, 6, 11))
    }
  }

  "flightsForPcpDateRange with UtcDates" should {
    implicit val system: ActorSystem = ActorSystem("FlightDaoTest")
    implicit val mat: Materializer = Materializer.matFromSystem
    "return flights at the beginning of a UtcDate in BST" in {
      checkScheduledIsReturnedForDate("2024-06-11T00:00:00Z", UtcDate(2024, 6, 11))
    }
    "return flights at the end of a UtcDate in BST" in {
      checkScheduledIsReturnedForDate("2024-06-11T23:45:00Z", UtcDate(2024, 6, 11))
    }
    "not return flights earlier than the beginning of a UtcDate in BST" in {
      checkScheduledIsNotReturnedForDate("2024-06-10T23:45:00Z", UtcDate(2024, 6, 11))
    }
    "not return flights later than the end of a UtcDate in BST" in {
      checkScheduledIsNotReturnedForDate("2024-06-12T00:00:00Z", UtcDate(2024, 6, 11))
    }
  }

  private def checkScheduledIsReturnedForDate(scheduled: String, date: DateLike)
                                             (implicit mat: Materializer): Any = {
    val (arrival: Arrival, arrivals: Seq[Arrival]) = insertAndQueryForDate(scheduled, date)

    arrivals should contain(arrival)
  }

  private def checkScheduledIsNotReturnedForDate(scheduled: String, date: DateLike)
                                             (implicit mat: Materializer): Any = {
    val (arrival: Arrival, arrivals: Seq[Arrival]) = insertAndQueryForDate(scheduled, date)

    arrivals should not contain(arrival)
  }

  private def insertAndQueryForDate(scheduled: String, date: DateLike)
                                   (implicit mat: Materializer)= {
    val arrival = ArrivalGenerator.arrival("BA123", scheduled, terminal = T1, origin = PortCode("JFK"), feedSource = LiveFeedSource)
    val flight1 = ApiFlightWithSplits(arrival, Set())

    Await.result(TestDatabase.run(dao.insertOrUpdate(portCode)(flight1)), 2.second)

    val future = dao.flightsForPcpDateRange(portCode, List(LiveFeedSource), TestDatabase.run)(date, date, Seq(T1)).runWith(Sink.seq)
    val arrivals = Await.result(future, 2.second).flatMap(_._2.map(_.apiFlight))
    (arrival, arrivals)
  }
}
