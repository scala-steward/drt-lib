package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.tables.{PassengersHourly, PassengersHourlyRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

object PassengersHourlySerialiser {
  val toRow: (PassengersHourly, Long) => PassengersHourlyRow = {
    case (PassengersHourly(portCode, terminal, queue, dateUtc, hour, passengers), updatedAt) =>
      PassengersHourlyRow(
        portCode.iata,
        terminal.toString,
        queue.toString,
        dateUtc.toISOString,
        hour,
        passengers,
        new Timestamp(updatedAt),
      )
  }

  val fromRow: PassengersHourlyRow => PassengersHourly = {
    case PassengersHourlyRow(portCode, terminal, queue, dateUtc, hour, passengers, _) =>
      PassengersHourly(
        PortCode(portCode),
        Terminal(terminal),
        Queue(queue),
        UtcDate.parse(dateUtc).getOrElse(throw new Exception(s"Could not parse date $dateUtc")),
        hour,
        passengers,
      )
  }
}
