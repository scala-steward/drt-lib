package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import uk.gov.homeoffice.drt.arrivals.{ApiFlightWithSplits, UniqueArrival}
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.FlightSerialiser
import uk.gov.homeoffice.drt.db.tables.{FlightRow, FlightTable}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp
import scala.concurrent.ExecutionContext


case class FlightDao()
                    (implicit ec: ExecutionContext) {
  val table: TableQuery[FlightTable] = TableQuery[FlightTable]

  def get(port: PortCode): (PortCode, Terminal, Long, Int) => DBIOAction[Option[ApiFlightWithSplits], NoStream, Effect.Read] =
    (origin, terminal, scheduled, voyageNumber) =>
      table
        .filter(f =>
          f.origin === origin.iata &&
            f.port === port.iata &&
            f.terminal === terminal.toString &&
            f.scheduled === new Timestamp(scheduled) &&
            f.voyageNumber === voyageNumber)
        .result
        .map(_.map(FlightSerialiser.fromRow).headOption)

  def getForTerminalDate(port: PortCode): (String, UtcDate) => DBIOAction[Seq[ApiFlightWithSplits], NoStream, Effect.Read] =
    (terminal, date) =>
      table
        .filter(f =>
          f.port === port.iata &&
            f.terminal === terminal &&
            f.scheduledDateUtc === date.toISOString
        )
        .result
        .map(_.map(FlightSerialiser.fromRow))

  def getForDateDate(port: PortCode): UtcDate => DBIOAction[Seq[ApiFlightWithSplits], NoStream, Effect.Read] =
    date =>
      table
        .filter(f => f.port === port.iata && f.scheduledDateUtc === date.toISOString)
        .result
        .map(_.map(FlightSerialiser.fromRow))

  def insertOrUpdate(port: PortCode): ApiFlightWithSplits => DBIOAction[Int, NoStream, Effect.Write with Effect.Transactional] = {
    val toRow: ApiFlightWithSplits => FlightRow = FlightSerialiser.toRow(port)
    flight =>
      table.insertOrUpdate(toRow(flight))
  }

  def insertOrUpdateMulti(port: PortCode): Iterable[ApiFlightWithSplits] => DBIOAction[Int, NoStream, Effect.Write with Effect.Transactional] = {
    val insertOrUpdateSingle = insertOrUpdate(port)
    flights =>
      DBIO.sequence(flights.map(insertOrUpdateSingle)).map(_.sum)
  }

  def remove(port: PortCode): UniqueArrival => DBIOAction[Int, NoStream, Effect.Write] =
    ua => remove(port, ua)

  def removeMulti(port: PortCode): Iterable[UniqueArrival] => DBIOAction[Int, NoStream, Effect.Write] =
    uas => {
      val queries = uas.map(ua => remove(port, ua))
      DBIO.sequence(queries).map(_.sum)
    }

  private def remove(port: PortCode, ua: UniqueArrival): DBIOAction[Int, NoStream, Effect.Write] =
    table
      .filter(f =>
        f.port === port.toString &&
          f.terminal === ua.terminal.toString &&
          f.origin === ua.origin.iata &&
          f.scheduled === new Timestamp(ua.scheduled) &&
          f.voyageNumber === ua.number)
      .delete
}
