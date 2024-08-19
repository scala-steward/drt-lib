package uk.gov.homeoffice.drt.db.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal

import java.sql.Timestamp


case class PortTerminalConfig(port: PortCode,
                              terminal: Terminal,
                              minimumRosteredStaff: Option[Int],
                              updatedAt: Long,
                             )

case class PortTerminalConfigRow(port: String,
                                 terminal: String,
                                 minimumRosteredStaff: Option[Int],
                                 updatedAt: Timestamp,
                                )

class PortTerminalConfigTable(tag: Tag) extends Table[PortTerminalConfigRow](tag, "port_terminal_config") {

  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def minimumRosteredStaff: Rep[Option[Int]] = column[Option[Int]]("minimum_rostered_staff")

  def updatedAt: Rep[Timestamp] = column[Timestamp]("updated_at")

  val pk = primaryKey("port_config_pkey", (port, terminal))

  def * = (port, terminal, minimumRosteredStaff, updatedAt) <> (PortTerminalConfigRow.tupled, PortTerminalConfigRow.unapply)
}
