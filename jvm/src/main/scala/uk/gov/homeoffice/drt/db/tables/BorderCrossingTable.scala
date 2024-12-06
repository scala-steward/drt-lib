package uk.gov.homeoffice.drt.db.tables

import slick.lifted.Tag
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

sealed trait GateType {
  def value: String
}

case object EGate extends GateType {
  val value: String = "egate"
}
case object Pcp extends GateType {
  val value: String = "pcp"
}

object GateType {
  def apply(value: String): GateType = value match {
    case EGate.value => EGate
    case Pcp.value => Pcp
    case _ => throw new IllegalArgumentException(s"Unknown gate type: $value")
  }
}

case class BorderCrossing(portCode: PortCode,
                          terminal: Terminal,
                          dateUtc: UtcDate,
                          gateType: GateType,
                          hour: Int,
                          passengers: Int,
                         )

case class BorderCrossingRow(portCode: String,
                             terminal: String,
                             dateUtc: String,
                             gateType: String,
                             hour: Int,
                             passengers: Int,
                             updatedAt: Timestamp,
                            )

class BorderCrossingTable(tag: Tag)
  extends Table[BorderCrossingRow](tag, "border_crossing") {

  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def dateUtc: Rep[String] = column[String]("date_utc")

  def gateType: Rep[String] = column[String]("gate_type")

  def hour: Rep[Int] = column[Int]("hour", O.SqlType("smallint"))

  def passengers: Rep[Int] = column[Int]("passengers", O.SqlType("smallint"))

  def updatedAt: Rep[Timestamp] = column[java.sql.Timestamp]("updated_at")

  def pk = primaryKey("pk_border_crossing_port_terminal_dateutc_hour", (port, terminal, dateUtc, hour))

  def portTerminalDateHourIndex = index("idx_border_crossing_port_terminal_date_hour", (port, terminal, dateUtc, hour), unique = false)

  def portTerminalDateIndex = index("idx_border_crossing_port_terminal_date", (port, terminal, dateUtc), unique = false)

  def portDateIndex = index("idx_border_crossing_port_date", (port, dateUtc), unique = false)

  def dateIndex = index("idx_border_crossing_date", dateUtc, unique = false)

  def * = (
    port,
    terminal,
    dateUtc,
    gateType,
    hour,
    passengers,
    updatedAt) <> (BorderCrossingRow.tupled, BorderCrossingRow.unapply)
}


