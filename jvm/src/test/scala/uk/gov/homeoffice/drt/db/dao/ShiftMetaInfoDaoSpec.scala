package uk.gov.homeoffice.drt.db.dao

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import uk.gov.homeoffice.drt.ShiftMeta
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.TestDatabase.profile.api._
import uk.gov.homeoffice.drt.db.tables.ShiftMetaInfoRow

import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class ShiftMetaInfoDaoSpec extends Specification with BeforeEach {
  sequential

  val dao: ShiftMetaInfoDao = ShiftMetaInfoDao(TestDatabase)

  override def before: Unit = {
    Await.result(
      TestDatabase.run(DBIO.seq(
        dao.shiftMetaInfoTable.schema.dropIfExists,
        dao.shiftMetaInfoTable.schema.createIfNotExists)
      ), 2.second)
  }

  val currentTimeInMillis: Long = Instant.now().toEpochMilli

  val currentTimestamp = new java.sql.Timestamp(currentTimeInMillis)

  def getShiftMeta: ShiftMeta =
    ShiftMeta(
      port = "LHR",
      terminal = "T5",
      shiftAssignmentsMigratedAt = Some(currentTimeInMillis)
    )

  def getShiftMetaRow: ShiftMetaInfoRow =
    ShiftMetaInfoRow(
      port = "LHR",
      terminal = "T5",
      shiftAssignmentsMigratedAt = Some(currentTimestamp)
    )

  "getShiftMetaInfo" should {
    "insert or select a shift meta info " in {
      val shiftMetaData = getShiftMeta

      val insertResult = Await.result(dao.insertShiftMetaInfo(shiftMetaData), 1.second)

      insertResult === 1

      val selectResult: Option[ShiftMeta] = Await.result(dao.getShiftMetaInfo("LHR", "T5"), 1.second)

      selectResult.get === shiftMetaData
    }

    "update shift meta info shiftAssignmentsMigratedAt column" in {
      val shiftMetaData = getShiftMeta

      val insertResult = Await.result(dao.insertShiftMetaInfo(shiftMetaData), 1.second)

      insertResult === 1

      val updatedShiftAssignmentsMigratedAt = currentTimeInMillis + 20000

      val updateResult: Option[ShiftMeta] = Await.result(dao.updateShiftAssignmentsMigratedAt("LHR", "T5", Some(updatedShiftAssignmentsMigratedAt)), 1.second)

      val expectedUpdatedShiftMetaData = shiftMetaData.copy(shiftAssignmentsMigratedAt = Some(updatedShiftAssignmentsMigratedAt))
      updateResult.get === expectedUpdatedShiftMetaData

      val selectResult: Option[ShiftMeta] = Await.result(dao.getShiftMetaInfo("LHR", "T5"), 1.second)
      selectResult.get === expectedUpdatedShiftMetaData
    }
  }
}