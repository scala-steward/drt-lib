package uk.gov.homeoffice.drt.db.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Tag}

case class ShiftStaffRollingRow(
                                 port: String,
                                 terminal: String,
                                 rollingStartDate: java.sql.Date,
                                 rollingEndDate: java.sql.Date,
                                 updatedAt: java.sql.Timestamp,
                                 triggeredBy: String
                               )


class ShiftStaffRollingTable(_tableTag: Tag) extends Table[ShiftStaffRollingRow](_tableTag, "shift_staff_rolling") {
  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def rollingStartDate: Rep[java.sql.Date] = column[java.sql.Date]("rolling_start_date")

  def rollingEndDate: Rep[java.sql.Date] = column[java.sql.Date]("rolling_end_date")

  def updatedAt: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("updated_at")

  def triggeredBy: Rep[String] = column[String]("triggered_by")

  val pk = primaryKey("shift_staff_rolling_pkey", (port, terminal, rollingStartDate))

  override def * : ProvenShape[ShiftStaffRollingRow] = (port, terminal, rollingStartDate, rollingEndDate, updatedAt, triggeredBy).mapTo[ShiftStaffRollingRow]
}
