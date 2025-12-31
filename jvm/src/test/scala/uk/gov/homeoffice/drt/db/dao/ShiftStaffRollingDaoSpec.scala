package uk.gov.homeoffice.drt.db.dao

import org.joda.time.DateTimeZone
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import uk.gov.homeoffice.drt.ShiftStaffRolling
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.TestDatabase.profile.api._
import uk.gov.homeoffice.drt.db.tables.ShiftStaffRollingRow
import uk.gov.homeoffice.drt.time.SDate

import java.time.Instant
import java.util.TimeZone
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Try

class ShiftStaffRollingDaoSpec extends Specification with BeforeEach {
  sequential
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  val dao: ShiftStaffRollingDao = ShiftStaffRollingDao(TestDatabase)

  override def before: Unit = {
    Try {
      Await.result(TestDatabase.run(dao.shiftStaffRollingTable.schema.createIfNotExists), 10.seconds)
    }

    Await.result(TestDatabase.run(dao.shiftStaffRollingTable.delete), 10.seconds)
  }

  val startDate = SDate("2024-06-01", DateTimeZone.forID("Europe/London")).millisSinceEpoch
  val endDate = SDate("2024-06-02", DateTimeZone.forID("Europe/London")).millisSinceEpoch

  def getShiftStaffRolling: ShiftStaffRolling = {
    val currentTimeInMillis = SDate.now().millisSinceEpoch
    ShiftStaffRolling(
      port = "LHR",
      terminal = "T5",
      rollingStartDate = startDate,
      rollingEndDate = endDate,
      updatedAt = currentTimeInMillis,
      triggeredBy = "auto-roll"
    )
  }

  def getShiftStaffRollingRow: ShiftStaffRollingRow = {
    val currentTimeInMillis = SDate.now().millisSinceEpoch
    ShiftStaffRollingRow(
      port = "LHR",
      terminal = "T5",
      rollingStartDate = new java.sql.Date(startDate),
      rollingEndDate = new java.sql.Date(endDate),
      updatedAt = new java.sql.Timestamp(currentTimeInMillis),
      triggeredBy = "auto-roll"
    )
  }

  "getShiftMetaInfo" should {
    "insert or select a shift meta info " in {
      val shiftStaffRolling = getShiftStaffRolling

      val insertResult = Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling), 5.seconds)

      insertResult === 1

      val selectResult: Seq[ShiftStaffRolling] = Await.result(dao.getShiftStaffRolling("LHR", "T5"), 5.seconds)

      selectResult === Seq(shiftStaffRolling)
    }

    "insert multiple port and get for one port" in {
      val shiftStaffRolling = getShiftStaffRolling
      val shiftStaffRolling2 = shiftStaffRolling.copy(terminal = "T1", rollingStartDate = Instant.now().toEpochMilli, rollingEndDate = Instant.now().toEpochMilli)
      val shiftStaffRolling3 = shiftStaffRolling.copy(port = "LGW", terminal = "T1", rollingStartDate = Instant.now().toEpochMilli, rollingEndDate = Instant.now().toEpochMilli)
      val shiftStaffRolling4 = shiftStaffRolling.copy(port = "LGW", terminal = "T2", rollingStartDate = Instant.now().toEpochMilli, rollingEndDate = Instant.now().toEpochMilli)

      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling), 5.seconds)
      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling2), 5.seconds)
      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling3), 5.seconds)
      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling4), 5.seconds)

      val selectResult: Seq[ShiftStaffRolling] = Await.result(dao.getShiftStaffRolling("LHR", "T5"), 5.seconds)

      selectResult === Seq(shiftStaffRolling)
    }

    "insert multiple records and get the latest one" in {
      val currentTimeInMillis = SDate.now().millisSinceEpoch
      val shiftStaffRolling = getShiftStaffRolling.copy(port = "MAN", updatedAt = currentTimeInMillis)
      val shiftStaffRolling2 = shiftStaffRolling.copy(port = "MAN", updatedAt = currentTimeInMillis + 1000,
        rollingStartDate = SDate("2024-06-01", DateTimeZone.forID("Europe/London")).millisSinceEpoch,
        rollingEndDate = SDate("2024-06-05", DateTimeZone.forID("Europe/London")).millisSinceEpoch)
      val shiftStaffRolling3 = shiftStaffRolling.copy(port = "MAN", updatedAt = currentTimeInMillis + 2000,
        rollingStartDate = SDate("2024-06-06", DateTimeZone.forID("Europe/London")).millisSinceEpoch,
        rollingEndDate = SDate("2024-06-10", DateTimeZone.forID("Europe/London")).millisSinceEpoch)

      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling), 5.seconds)
      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling2), 5.seconds)
      Await.result(dao.upsertShiftStaffRolling(shiftStaffRolling3), 5.seconds)

      val selectResult: Option[ShiftStaffRolling] = Await.result(dao.latestShiftStaffRolling("MAN", "T5"), 5.seconds)

      selectResult === Option(shiftStaffRolling3)
    }
  }
}
