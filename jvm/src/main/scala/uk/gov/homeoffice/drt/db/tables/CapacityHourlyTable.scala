package uk.gov.homeoffice.drt.db.tables

import slick.lifted.Tag
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

case class CapacityHourly(portCode: PortCode,
                          terminal: Terminal,
                          dateUtc: UtcDate,
                          hour: Int,
                          capacity: Int,
                         )

case class CapacityHourlyRow(portCode: String,
                             terminal: String,
                             dateUtc: String,
                             hour: Int,
                             capacity: Int,
                             updatedAt: Timestamp,
                            )

class CapacityHourlyTable(tag: Tag)
  extends Table[CapacityHourlyRow](tag, "capacity_hourly") {

  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def dateUtc: Rep[String] = column[String]("date_utc")

  def hour: Rep[Int] = column[Int]("hour", O.SqlType("smallint"))

  def capacity: Rep[Int] = column[Int]("capacity", O.SqlType("smallint"))

  def updatedAt: Rep[Timestamp] = column[java.sql.Timestamp]("updated_at")

  def pk = primaryKey("pk_capacity_hourly_port_terminal_dateutc_hour", (port, terminal, dateUtc, hour))

  def portTerminalDateHourIndex = index("idx_capacity_hourly_port_terminal_date_hour", (port, terminal, dateUtc, hour), unique = false)

  def portTerminalDateIndex = index("idx_capacity_hourly_port_terminal_date", (port, terminal, dateUtc), unique = false)

  def portDateIndex = index("idx_capacity_hourly_port_date", (port, dateUtc), unique = false)

  def dateIndex = index("idx_capacity_hourly_date", dateUtc, unique = false)

  def * = (
    port,
    terminal,
    dateUtc,
    hour,
    capacity,
    updatedAt) <> (CapacityHourlyRow.tupled, CapacityHourlyRow.unapply)
}


