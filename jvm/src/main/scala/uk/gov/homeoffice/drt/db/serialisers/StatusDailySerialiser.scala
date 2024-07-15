package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.{StatusDaily, StatusDailyRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.LocalDate

import java.sql.Timestamp

object StatusDailySerialiser {
  val toRow: StatusDaily => StatusDailyRow = {
    case StatusDaily(portCode, terminal, dateLocal, paxUpdated, deskRecsUpdated, deploymentsUpdated) =>
      StatusDailyRow(
        portCode.iata,
        terminal.toString,
        dateLocal.toISOString,
        paxUpdated.map(new Timestamp(_)),
        deskRecsUpdated.map(new Timestamp(_)),
        deploymentsUpdated.map(new Timestamp(_)),
      )
  }

  val fromRow: StatusDailyRow => StatusDaily = {
    case StatusDailyRow(portCode, terminal, dateLocal, paxUpdatedAt, deskRecsUpdatedAt, deploymentsUpdatedAt) =>
      StatusDaily(
        PortCode(portCode),
        Terminal(terminal),
        LocalDate.parse(dateLocal).getOrElse(throw new Exception(s"Could not parse date $dateLocal")),
        paxUpdatedAt.map(_.getTime),
        deskRecsUpdatedAt.map(_.getTime),
        deploymentsUpdatedAt.map(_.getTime),
      )
  }
}
