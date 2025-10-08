package uk.gov.homeoffice.drt.db.dao

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import uk.gov.homeoffice.drt.ShiftStaffRolling
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.TestDatabase.profile.api._
import uk.gov.homeoffice.drt.db.tables.ShiftStaffRollingRow
import uk.gov.homeoffice.drt.time.SDate
import uk.gov.homeoffice.drt.time.TimeZoneHelper.europeLondonTimeZone

import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class ShiftStaffRollingDaoSpec extends Specification with BeforeEach {
  sequential

  val dao: ShiftStaffRollingDao = ShiftStaffRollingDao(TestDatabase)

  override def before: Unit = {
    Await.result(
      TestDatabase.run(DBIO.seq(
        dao.shiftStaffRollingTable.schema.dropIfExists,
        dao.shiftStaffRollingTable.schema.createIfNotExists)
      ), 2.second)
  }

  val currentTimeInMillis: Long = SDate.now().millisSinceEpoch

  val startDate = SDate("2024-06-01").millisSinceEpoch
  val endDate = SDate("2024-06-02").millisSinceEpoch

  def getShiftStaffRolling: ShiftStaffRolling =
    ShiftStaffRolling(
      port = "LHR",
      terminal = "T5",
      rollingStartDate = startDate,
      rollingEndDate = endDate,
      updatedAt = currentTimeInMillis,
      triggeredBy = "auto-roll"
    )

  def getShiftStaffRollingRow: ShiftStaffRollingRow =
    ShiftStaffRollingRow(
      port = "LHR",
      terminal = "T5",
      rollingStartDate = new java.sql.Date(startDate),
      rollingEndDate = new java.sql.Date(endDate),
      updatedAt = new java.sql.Timestamp(currentTimeInMillis),
      triggeredBy = "auto-roll"
    )

  "getShiftMetaInfo" should {
    "insert or select a shift meta info " in {
      val shiftStaffRolling = getShiftStaffRolling

      val insertResult = Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling), 1.second)

      insertResult === 1

      val selectResult: Seq[ShiftStaffRolling] = Await.result(dao.getShiftStaffRolling("LHR", "T5"), 1.second)

      selectResult.head === shiftStaffRolling
    }

    "insert multiple port and get for one port" in {
      val shiftStaffRolling = getShiftStaffRolling
      val shiftStaffRolling2 = shiftStaffRolling.copy(terminal = "T1", rollingStartDate = Instant.now().toEpochMilli, rollingEndDate = Instant.now().toEpochMilli)
      val shiftStaffRolling3 = shiftStaffRolling.copy(port = "LGW", terminal = "T1", rollingStartDate = Instant.now().toEpochMilli, rollingEndDate = Instant.now().toEpochMilli)
      val shiftStaffRolling4 = shiftStaffRolling.copy(port = "LGW", terminal = "T2", rollingStartDate = Instant.now().toEpochMilli, rollingEndDate = Instant.now().toEpochMilli)

      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling), 1.second)
      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling2), 1.second)
      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling3), 1.second)
      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling4), 1.second)

      val selectResult: Seq[ShiftStaffRolling] = Await.result(dao.getShiftStaffRolling("LHR", "T5"), 1.second)

      selectResult.head === shiftStaffRolling
    }
  }
}
