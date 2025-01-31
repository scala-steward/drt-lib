package uk.gov.homeoffice.drt.db.tables

import java.sql.{Date, Timestamp}
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

case class StaffShiftRow(
                          port: String,
                          terminal: String,
                          shiftName: String,
                          startDate: Date,
                          startTime: String,
                          endTime: String,
                          endDate: Option[Date],
                          staffNumber: Int,
                          createdBy: Option[String],
                          frequency: Option[String],
                          createdAt: Timestamp
                        )

class StaffShiftsTable(tag: Tag) extends Table[StaffShiftRow](tag, "staff_shifts") {
  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def startDate: Rep[Date] = column[Date]("start_date")

  def shiftName: Rep[String] = column[String]("shift_name")

  def startTime: Rep[String] = column[String]("start_time")

  def endTime: Rep[String] = column[String]("end_time")

  def endDate: Rep[Option[Date]] = column[Option[Date]]("end_date")

  def staffNumber: Rep[Int] = column[Int]("staff_number")

  def createdBy: Rep[Option[String]] = column[Option[String]]("created_by")

  def frequency: Rep[Option[String]] = column[Option[String]]("frequency")

  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at")

  val pk = primaryKey("staff_shifts_pkey", (port, terminal, shiftName, startDate, startTime))

  def * = (
    port,
    terminal,
    shiftName,
    startDate,
    startTime,
    endTime,
    endDate,
    staffNumber,
    createdBy,
    frequency,
    createdAt
  ).mapTo[StaffShiftRow]
}