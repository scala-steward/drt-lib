package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.{StatusDaily, StatusDailyRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

object StatusDailySerialiser {
  val toRow: StatusDaily => StatusDailyRow = {
    case StatusDaily(portCode, terminal, dateUtc, paxUpdated, deskRecsUpdated, deploymentsUpdated) =>
      StatusDailyRow(
        portCode.iata,
        terminal.toString,
        dateUtc.toISOString,
        new Timestamp(paxUpdated),
        new Timestamp(deskRecsUpdated),
        new Timestamp(deploymentsUpdated),
      )
  }

  val fromRow: StatusDailyRow => StatusDaily = {
    case StatusDailyRow(portCode, terminal, dateUtc, paxUpdatedAt, deskRecsUpdatedAt, deploymentsUpdatedAt) =>
      StatusDaily(
        PortCode(portCode),
        Terminal(terminal),
        UtcDate.parse(dateUtc).getOrElse(throw new Exception(s"Could not parse date $dateUtc")),
        paxUpdatedAt.getTime,
        deskRecsUpdatedAt.getTime,
        deploymentsUpdatedAt.getTime,
      )
  }
}
