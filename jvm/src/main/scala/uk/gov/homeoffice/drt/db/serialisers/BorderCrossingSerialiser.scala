package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.tables.{BorderCrossing, BorderCrossingRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

object BorderCrossingSerialiser {
  val toRow: (BorderCrossing, Long) => BorderCrossingRow = {
    case (BorderCrossing(portCode, terminal, dateUtc, hour, passengers), updatedAt) =>
      BorderCrossingRow(
        portCode.iata,
        terminal.toString,
        dateUtc.toISOString,
        hour,
        passengers,
        new Timestamp(updatedAt),
      )
  }

  val fromRow: BorderCrossingRow => BorderCrossing = {
    case BorderCrossingRow(portCode, terminal, dateUtc, hour, passengers, _) =>
      BorderCrossing(
        PortCode(portCode),
        Terminal(terminal),
        UtcDate.parse(dateUtc).getOrElse(throw new Exception(s"Could not parse date $dateUtc")),
        hour,
        passengers,
      )
  }
}
