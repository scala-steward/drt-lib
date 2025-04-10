package uk.gov.homeoffice.drt.db.dao

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import uk.gov.homeoffice.drt.db.CentralDatabase
import uk.gov.homeoffice.drt.db.tables.{StaffShiftRow, StaffShiftsTable}

import scala.concurrent.Future

trait IStaffShiftsDao {
  def insertOrUpdate(staffShiftRow: StaffShiftRow): Future[Int]

  def getStaffShiftsByPort(port: String): Future[Seq[StaffShiftRow]]

  def getStaffShiftsByPortAndTerminal(port: String, terminal: String): Future[Seq[StaffShiftRow]]

  def getStaffShiftByPortAndTerminalAndShiftName(port: String, terminal: String, shiftName: String): Future[Option[StaffShiftRow]]

  def deleteStaffShift(port: String, terminal: String, shiftName: String): Future[Int]

  def deleteStaffShifts(): Future[Int]
}

case class StaffShiftsDao(db: CentralDatabase) extends IStaffShiftsDao {
  val staffShiftsTable: TableQuery[StaffShiftsTable] = TableQuery[StaffShiftsTable]

  override def insertOrUpdate(staffShiftRow: StaffShiftRow): Future[Int] =
    db.run(staffShiftsTable.insertOrUpdate(staffShiftRow))

  override def getStaffShiftsByPort(port: String): Future[Seq[StaffShiftRow]] =
    db.run(staffShiftsTable.filter(_.port === port).result)

  override def getStaffShiftsByPortAndTerminal(port: String, terminal: String): Future[Seq[StaffShiftRow]] =
    db.run(staffShiftsTable.filter(s => s.port === port && s.terminal === terminal).result)

  override def getStaffShiftByPortAndTerminalAndShiftName(port: String, terminal: String, shiftName: String): Future[Option[StaffShiftRow]] =
    db.run(staffShiftsTable.filter(s => s.port === port && s.terminal === terminal && s.shiftName.toLowerCase === shiftName.toLowerCase).result.headOption)

  override def deleteStaffShift(port: String, terminal: String, shiftName: String): Future[Int] =
    db.run(staffShiftsTable.filter(row => row.port === port && row.terminal === terminal && row.shiftName === shiftName).delete)

  override def deleteStaffShifts(): Future[Int] = db.run(staffShiftsTable.delete)
}
