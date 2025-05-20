package uk.gov.homeoffice.drt.db.dao

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import slick.dbio.DBIO
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.TestDatabase.profile
import uk.gov.homeoffice.drt.db.tables.StaffShiftRow
import uk.gov.homeoffice.drt.time.SDate

import java.sql.{Date, Timestamp}
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import TestDatabase.profile.api._

class StaffShiftsDaoSpec extends Specification with BeforeEach {
  sequential

  val dao: StaffShiftsDao = StaffShiftsDao(TestDatabase)
  override def before: Unit = {
    Await.result(
      TestDatabase.run(DBIO.seq(
        dao.staffShiftsTable.schema.dropIfExists,
        dao.staffShiftsTable.schema.createIfNotExists)
      ), 2.second)
  }

  def getStaffShiftRow: StaffShiftRow = {
    StaffShiftRow(
      port = "LHR",
      terminal = "T5",
      shiftName = "Morning",
      startDate = new Date(SDate("2021-01-01").millisSinceEpoch),
      startTime = "08:00",
      endTime = "16:00",
      endDate = None,
      staffNumber = 10,
      createdBy = Some("test@drt.com"),
      frequency = Some("Daily"),
      createdAt = new Timestamp(Instant.now().toEpochMilli)
    )
  }

  "StaffShiftsDao" should {
    "insert or update a staff shift" in {
      val staffShiftsDao = StaffShiftsDao(TestDatabase)
      val staffShiftRow = getStaffShiftRow

      val insertResult = Await.result(staffShiftsDao.insertOrUpdate(staffShiftRow), 1.second)
      insertResult === 1

      val selectResult = Await.result(staffShiftsDao.getStaffShiftsByPortAndTerminal("LHR","T5"), 1.second)

      selectResult === Seq(staffShiftRow)
    }

    "retrieve staff shifts by port" in {
      val staffShiftsDao = StaffShiftsDao(TestDatabase)
      val staffShiftRow = getStaffShiftRow

      Await.result(staffShiftsDao.insertOrUpdate(staffShiftRow), 1.second)

      val selectResult = Await.result(staffShiftsDao.getStaffShiftsByPort("LHR"), 1.second)
      selectResult.size === 1
      selectResult.head === staffShiftRow
    }

    "delete a staff shift" in {
      val staffShiftsDao = StaffShiftsDao(TestDatabase)
      val staffShiftRow = getStaffShiftRow

      Await.result(staffShiftsDao.insertOrUpdate(staffShiftRow), 1.second)

      val deleteResult = Await.result(staffShiftsDao.deleteStaffShift("LHR", "T5", "Morning"), 1.second)
      deleteResult === 1

      val selectResult = Await.result(staffShiftsDao.getStaffShiftsByPortAndTerminal("LHR","T5"), 1.second)
      selectResult.isEmpty === true
    }
  }
}
