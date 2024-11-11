package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import specs2.arguments.sequential
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.model.CrunchMinute
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.EeaDesk
import uk.gov.homeoffice.drt.ports.Terminals.T2
import uk.gov.homeoffice.drt.time.{SDate, UtcDate}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class QueueSlotDaoTest extends AnyWordSpec  with Matchers with BeforeAndAfter {
  sequential

  private val db = TestDatabase.db

  import TestDatabase.profile.api._

  val dao: QueueSlotDao = QueueSlotDao()

  println(dao.table.schema.createStatements.mkString(";\n") + ";")

  before {
    Await.result(
      db.run(DBIO.seq(
        dao.table.schema.dropIfExists,
        dao.table.schema.createIfNotExists)
      ), 2.second)
  }

  "insertOrUpdate" should {
    "insert records into an empty table" in {
      val crunchMinute = CrunchMinute(
        terminal = T2,
        queue = EeaDesk,
        minute = 1L,
        paxLoad = 100.11,
        workLoad = 99.22,
        deskRec = 98,
        waitTime = 97,
        maybePaxInQueue = Option(100),
        deployedDesks = Option(101),
        deployedWait = Option(102),
        maybeDeployedPaxInQueue = Option(103),
        lastUpdated = Option(100L)
      )

      Await.result(db.run(dao.insertOrUpdate(PortCode("LHR"), 15)(crunchMinute)), 2.second)

      val rows = Await.result(db.run(dao.get(PortCode("LHR"), 15)(T2, EeaDesk, 1L)), 1.second)
      rows should be(Seq(crunchMinute))
    }

    "existing queues are replaced with new records for same time periods" in {
      val crunchMinute = CrunchMinute(
        terminal = T2,
        queue = EeaDesk,
        minute = 1L,
        paxLoad = 100,
        workLoad = 99,
        deskRec = 98,
        waitTime = 97,
        maybePaxInQueue = Option(100),
        deployedDesks = Option(101),
        deployedWait = Option(102),
        maybeDeployedPaxInQueue = Option(103),
        lastUpdated = Option(100L)
      )
      val crunchMinute2 = crunchMinute.copy(
        paxLoad = 101,
        workLoad = 100,
        deskRec = 99,
        waitTime = 98,
        maybePaxInQueue = Option(101),
        deployedDesks = Option(102),
        deployedWait = Option(103),
        maybeDeployedPaxInQueue = Option(104),
      )

      Await.result(db.run(dao.insertOrUpdate(PortCode("LHR"), 15)(crunchMinute)), 2.second)
      Await.result(db.run(dao.insertOrUpdate(PortCode("LHR"), 15)(crunchMinute2)), 2.second)

      val rows = Await.result(db.run(dao.get(PortCode("LHR"), 15)(T2, EeaDesk, 1L)), 1.second)
      rows should be(Seq(crunchMinute2))
    }
  }

  "removeAllBefore" should {
    "remove all records before the given date" in {
      val crunchMinutes = Seq(
        SDate("2024-11-11").millisSinceEpoch,
        SDate("2024-11-12").millisSinceEpoch,
        SDate("2024-11-13").millisSinceEpoch,
      ).map(crunchMinute)

      Await.result(db.run(dao.insertOrUpdateMulti(PortCode("LHR"), 15)(crunchMinutes)), 2.second)

      Await.result(db.run(dao.removeAllBefore(UtcDate(2024, 11, 13))), 2.second)

      Seq(
        (SDate("2024-11-11").millisSinceEpoch, Seq.empty),
        (SDate("2024-11-12").millisSinceEpoch, Seq.empty),
        (SDate("2024-11-13").millisSinceEpoch, Seq(crunchMinute(SDate("2024-11-13").millisSinceEpoch))),
      ).map {
        case (minute, expected) =>
          val rows = Await.result(db.run(dao.get(PortCode("LHR"), 15)(T2, EeaDesk, minute)), 1.second)
          rows should be(expected)
      }
    }
  }

  def crunchMinute(minute: Long): CrunchMinute = CrunchMinute(
    terminal = T2,
    queue = EeaDesk,
    minute = minute,
    paxLoad = 100,
    workLoad = 99,
    deskRec = 98,
    waitTime = 97,
    maybePaxInQueue = Option(100),
    deployedDesks = Option(101),
    deployedWait = Option(102),
    maybeDeployedPaxInQueue = Option(103),
    lastUpdated = Option(100L)
  )

}
