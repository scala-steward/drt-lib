package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.StatusDailySerialiser
import uk.gov.homeoffice.drt.db.{StatusDaily, StatusDailyRow, StatusDailyTable}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.LocalDate

import java.sql.Timestamp
import scala.concurrent.ExecutionContext


object StatusDailyDao {
  val table: TableQuery[StatusDailyTable] = TableQuery[StatusDailyTable]

  def insertOrUpdate(portCode: PortCode): StatusDaily => DBIOAction[Int, NoStream, Effect.Write] =
    row =>
      if (row.portCode == portCode) table.insertOrUpdate(StatusDailySerialiser.toRow(row))
      else DBIO.successful(0)

  def setUpdatedAt(portCode: PortCode): (StatusDailyTable => Rep[Option[Timestamp]]) => (Terminal, LocalDate, Long) => DBIOAction[Int, NoStream, Effect.Write] =
    columnToSet => (terminal, date, updatedAt) =>
      filterPortTerminalDate(portCode, terminal, date)
        .map(columnToSet)
        .update(Option(new Timestamp(updatedAt)))

  def get(portCode: PortCode)
         (implicit ec: ExecutionContext): (Terminal, LocalDate) => DBIOAction[Option[StatusDaily], NoStream, Effect.Read] =
    (terminal, date) =>
      filterPortTerminalDate(portCode, terminal, date)
        .result
        .map { rows =>
          rows.map(StatusDailySerialiser.fromRow).headOption
        }

  private def filterPortTerminalDate(portCode: PortCode, terminal: Terminal, date: LocalDate): Query[StatusDailyTable, StatusDailyRow, Seq] =
    table
      .filter(row =>
        row.port === portCode.iata &&
          row.terminal === terminal.toString &&
          row.dateLocal === date.toISOString
      )
}
