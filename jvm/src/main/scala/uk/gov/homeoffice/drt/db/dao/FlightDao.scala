package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import uk.gov.homeoffice.drt.arrivals.{ApiFlightWithSplits, UniqueArrival}
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.FlightSerialiser
import uk.gov.homeoffice.drt.db.tables.{FlightRow, FlightTable}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{DateLike, SDate, UtcDate}

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

  def getForTerminalDatePcpTime(port: PortCode): (Terminal, DateLike) => DBIOAction[Seq[ApiFlightWithSplits], NoStream, Effect.Read] =
    (terminal, date) =>
      filterDatePcpTime(date)
        .filter(f => f.port === port.iata && f.terminal === terminal.toString)
        .result
        .map(_.map(FlightSerialiser.fromRow))

  def getForDatePcpTime(port: PortCode): DateLike => DBIOAction[Seq[ApiFlightWithSplits], NoStream, Effect.Read] =
    date =>
      filterDatePcpTime(date)
        .filter(f => f.port === port.iata)
        .result
        .map(_.map(FlightSerialiser.fromRow))

  private def filterDatePcpTime(date: DateLike): Query[FlightTable, FlightRow, Seq] =
    table
      .filter { f =>
        val start = SDate(date)
        val end = start.addDays(1).addMinutes(-1)
        val dates = Set(start.toUtcDate, end.toUtcDate).map(_.toISOString)

        f.scheduledDateUtc.inSet(dates) && f.pcpTime >= new Timestamp(start.millisSinceEpoch) && f.pcpTime <= new Timestamp(end.millisSinceEpoch)
      }

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

  def removeAllBefore: UtcDate => DBIOAction[Int, NoStream, Effect.Write] = date =>
    table
      .filter(_.scheduledDateUtc < date.toISOString)
      .delete
}
