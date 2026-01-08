package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.models.CrunchMinute
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.EeaDesk
import uk.gov.homeoffice.drt.ports.Terminals.T2
import uk.gov.homeoffice.drt.time.{SDate, UtcDate}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class QueueSlotDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  import TestDatabase.profile.api._

  val dao: QueueSlotDao = QueueSlotDao()

  SchemaUtils.printStatements(dao.table.schema.createStatements)

  before {
    Await.result(
      TestDatabase.run(DBIO.seq(
        dao.table.schema.dropIfExists,
        dao.table.schema.createIfNotExists)
      ), 2.second)
  }

  "insertOrUpdate" should {
    val minuteMillis = SDate(UtcDate(2025, 1, 1)).millisSinceEpoch
    "insert records into an empty table" in {
      val crunchMinute = CrunchMinute(
        terminal = T2,
        queue = EeaDesk,
        minute = minuteMillis,
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

      Await.result(TestDatabase.run(dao.insertOrUpdate(PortCode("LHR"), 15)(crunchMinute)), 2.second)

      val rows = Await.result(TestDatabase.run(dao.get(PortCode("LHR"), 15)(T2, EeaDesk, minuteMillis)), 1.second)
      rows should be(Seq(crunchMinute))
    }

    "existing queues are replaced with new records for same time periods" in {
      val crunchMinute = CrunchMinute(
        terminal = T2,
        queue = EeaDesk,
        minute = minuteMillis,
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

      Await.result(TestDatabase.run(dao.insertOrUpdate(PortCode("LHR"), 15)(crunchMinute)), 2.second)
      Await.result(TestDatabase.run(dao.insertOrUpdate(PortCode("LHR"), 15)(crunchMinute2)), 2.second)

      val rows = Await.result(TestDatabase.run(dao.get(PortCode("LHR"), 15)(T2, EeaDesk, minuteMillis)), 1.second)
      rows should be(Seq(crunchMinute2))
    }
  }

  "removeAllBefore" should {
    "remove all records before the given date" in {
      val dates = Seq(
        UtcDate(2024, 11, 11),
        UtcDate(2024, 11, 12),
        UtcDate(2024, 11, 13),
      )

      val crunchMinutes = dates.map(d => crunchMinute(SDate(d).millisSinceEpoch))

      Await.result(TestDatabase.run(dao.updateSlots(PortCode("LHR"), 15)(crunchMinutes)), 2.second)

      Await.result(TestDatabase.run(dao.removeAllBefore()(UtcDate(2024, 11, 13))), 2.second)

      dates.zipWithIndex.foreach {
        case (date, idx) =>
          val minute = SDate(date).millisSinceEpoch
          val expected =
            if (idx < 2) Seq.empty
            else Seq(crunchMinute(minute))

          val rows = Await.result(TestDatabase.run(dao.get(PortCode("LHR"), 15)(T2, EeaDesk, minute)), 1.second)
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
