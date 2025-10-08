package uk.gov.homeoffice.drt.db.dao

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import uk.gov.homeoffice.drt.ShiftStaffRolling
import uk.gov.homeoffice.drt.db.CentralDatabase
import uk.gov.homeoffice.drt.db.tables.{ShiftStaffRollingRow, ShiftStaffRollingTable}

import java.sql.{Date, Timestamp}
import scala.concurrent.{ExecutionContext, Future}


trait IShiftStaffRollingDaoLike {
  def upsertShiftStaffRolling(shiftStaffRolling: ShiftStaffRolling): Future[Int]

  def getShiftStaffRolling(port: String, terminal: String): Future[Seq[ShiftStaffRolling]]
}

case class ShiftStaffRollingDao(central: CentralDatabase)(implicit ex: ExecutionContext) extends IShiftStaffRollingDaoLike {
  val shiftStaffRollingTable: TableQuery[ShiftStaffRollingTable] = TableQuery[ShiftStaffRollingTable]


  def upsertShiftStaffRolling(shiftStaffRolling: ShiftStaffRolling): Future[Int] = {
    val row = ShiftStaffRollingRow(
      shiftStaffRolling.port,
      shiftStaffRolling.terminal,
      new Date(shiftStaffRolling.rollingStartDate),
      new Date(shiftStaffRolling.rollingEndDate),
      new Timestamp(shiftStaffRolling.updatedAt),
      shiftStaffRolling.triggeredBy
    )
    val insertOrUpdate = shiftStaffRollingTable
      .insertOrUpdate(row)
    central.db.run(insertOrUpdate)
  }


  override def getShiftStaffRolling(port: String, terminal: String): Future[Seq[ShiftStaffRolling]] = {
    val query = shiftStaffRollingTable.filter(row => row.port === port && row.terminal === terminal)
    central.db.run(query.result)
      .map(rows => rows.map(row => ShiftStaffRolling(
        row.port,
        row.terminal,
        row.rollingStartDate.getTime,
        row.rollingEndDate.getTime,
        row.updatedAt.getTime,
        row.triggeredBy)))
  }
}
