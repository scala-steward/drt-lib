package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.PortTerminalConfigSerialiser
import uk.gov.homeoffice.drt.db.tables.{PortTerminalConfig, PortTerminalConfigRow, PortTerminalConfigTable}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal

import scala.concurrent.ExecutionContext


object PortTerminalConfigDao {
  val table: TableQuery[PortTerminalConfigTable] = TableQuery[PortTerminalConfigTable]

  def insertOrUpdate(portCode: PortCode): PortTerminalConfig => DBIOAction[Int, NoStream, Effect.Write] =
    config => {
      if (config.port == portCode) table.insertOrUpdate(PortTerminalConfigSerialiser.toRow(config))
      else DBIO.successful(0)
    }

  def get(portCode: PortCode)
         (implicit ec: ExecutionContext): Terminal => DBIOAction[Option[PortTerminalConfig], NoStream, Effect.Read] =
    terminal =>
      filterPortTerminalDate(portCode, terminal)
        .result
        .map { rows =>
          rows.map(PortTerminalConfigSerialiser.fromRow).headOption
        }

  private def filterPortTerminalDate(portCode: PortCode, terminal: Terminal): Query[PortTerminalConfigTable, PortTerminalConfigRow, Seq] =
    table
      .filter(row =>
        row.port === portCode.iata &&
          row.terminal === terminal.toString
      )
}
