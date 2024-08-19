package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.tables.{PortTerminalConfig, PortTerminalConfigRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal

import java.sql.Timestamp

object PortTerminalConfigSerialiser {
  val toRow: PortTerminalConfig => PortTerminalConfigRow = {
    case PortTerminalConfig(portCode, terminal, minimumRosteredStaff, updatedAt) =>
      PortTerminalConfigRow(
        portCode.iata,
        terminal.toString,
        minimumRosteredStaff,
        new Timestamp(updatedAt),
      )
  }

  val fromRow: PortTerminalConfigRow => PortTerminalConfig = {
    case PortTerminalConfigRow(port, terminal, minimumRosteredStaff, updatedAt) =>
      PortTerminalConfig(
        PortCode(port),
        Terminal(terminal),
        minimumRosteredStaff,
        updatedAt.getTime,
      )
  }
}
