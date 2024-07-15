package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import slick.sql.FixedSqlAction
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.StatusDailySerialiser
import uk.gov.homeoffice.drt.db.{StatusDaily, StatusDailyTable}
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

  def setUpdatedAt(portCode: PortCode): (Terminal, LocalDate, Long, StatusDailyTable => Rep[Timestamp]) => DBIOAction[Int, NoStream, Effect.Write] =
    (terminal, date, updatedAt, column) =>
      table
        .filter(_.port === portCode.iata)
        .filter(_.terminal === terminal.toString)
        .filter(_.dateUtc === date.toISOString)
        .map(column)
        .update(new Timestamp(updatedAt))

  def get(portCode: PortCode)
         (implicit ec: ExecutionContext): (Terminal, LocalDate) => DBIOAction[Option[StatusDaily], NoStream, Effect.Read] =
    (terminal, date) => table
      .filter(_.port === portCode.iata)
      .filter(_.terminal === terminal.toString)
      .filter(_.dateUtc === date.toISOString)
      .result
      .map { rows =>
        rows.map(StatusDailySerialiser.fromRow).headOption
      }
}
