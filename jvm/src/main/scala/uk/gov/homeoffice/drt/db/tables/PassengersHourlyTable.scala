package uk.gov.homeoffice.drt.db.tables

import slick.lifted.Tag
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

case class PassengersHourly(portCode: PortCode,
                            terminal: Terminal,
                            queue: Queue,
                            dateUtc: UtcDate,
                            hour: Int,
                            passengers: Int,
                           )

case class PassengersHourlyRow(portCode: String,
                               terminal: String,
                               queue: String,
                               dateUtc: String,
                               hour: Int,
                               passengers: Int,
                               updatedAt: Timestamp,
                              )

class PassengersHourlyTable(tag: Tag)
  extends Table[PassengersHourlyRow](tag, "passengers_hourly") {

  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def queue: Rep[String] = column[String]("queue")

  def dateUtc: Rep[String] = column[String]("date_utc")

  def hour: Rep[Int] = column[Int]("hour")

  def passengers: Rep[Int] = column[Int]("passengers")

  def updatedAt: Rep[Timestamp] = column[java.sql.Timestamp]("updated_at")

  def pk = primaryKey("pk_passengers_hourly_port_terminal_queue_dateutc_hour", (port, terminal, queue, dateUtc, hour))

  def * = (
    port,
    terminal,
    queue,
    dateUtc,
    hour,
    passengers,
    updatedAt) <> (PassengersHourlyRow.tupled, PassengersHourlyRow.unapply)
}


