package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.tables.{CapacityHourly, CapacityHourlyRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

object CapacityHourlySerialiser {
  val toRow: (CapacityHourly, Long) => CapacityHourlyRow = {
    case (CapacityHourly(portCode, terminal, dateUtc, hour, passengers), updatedAt) =>
      CapacityHourlyRow(
        portCode.iata,
        terminal.toString,
        dateUtc.toISOString,
        hour,
        passengers,
        new Timestamp(updatedAt),
      )
  }

  val fromRow: CapacityHourlyRow => CapacityHourly = {
    case CapacityHourlyRow(portCode, terminal, dateUtc, hour, passengers, _) =>
      CapacityHourly(
        PortCode(portCode),
        Terminal(terminal),
        UtcDate.parse(dateUtc).getOrElse(throw new Exception(s"Could not parse date $dateUtc")),
        hour,
        passengers,
      )
  }
}
