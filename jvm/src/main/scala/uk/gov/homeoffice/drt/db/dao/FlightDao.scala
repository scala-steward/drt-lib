package uk.gov.homeoffice.drt.db.dao

import akka.NotUsed
import akka.stream.scaladsl.Source
import slick.dbio.{DBIOAction, Effect, NoStream}
import uk.gov.homeoffice.drt.arrivals.{ApiFlightWithSplits, UniqueArrival}
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.FlightSerialiser
import uk.gov.homeoffice.drt.db.tables.{FlightRow, FlightTable}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}
import uk.gov.homeoffice.drt.time.{DateRange, LocalDate, SDate, UtcDate}

import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}


case class FlightDao()
                    (implicit ec: ExecutionContext) {
  val table: TableQuery[FlightTable] = TableQuery[FlightTable]

  def flightsForPcpDateRange(portCode: PortCode,
                             paxFeedSourceOrder: List[FeedSource],
                             execute: DBIOAction[(UtcDate, Seq[ApiFlightWithSplits]), NoStream, Effect.Read] => Future[(UtcDate, Seq[ApiFlightWithSplits])]
                            ): (LocalDate, LocalDate, Seq[Terminal]) => Source[(UtcDate, Seq[ApiFlightWithSplits]), NotUsed] = {
    val getFlights = getForTerminalsUtcDate(portCode)

    (start, end, terminals) =>
      val utcStart = SDate(start).addDays(-1).toUtcDate
      val endPlusADay = SDate(end).addDays(1).toUtcDate
      val utcEnd = UtcDate(endPlusADay.year, endPlusADay.month, endPlusADay.day)
      Source(DateRange(utcStart, utcEnd))
        .mapAsync(1) { date =>
          execute(
            getFlights(terminals, date)
              .map { flights =>
                val relevantFlightsForDates = flights.filter(_.apiFlight.hasPcpDuring(SDate(start), SDate(end).addDays(1).addMinutes(-1), paxFeedSourceOrder))
                date -> relevantFlightsForDates
              }
          )
        }
  }

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

  def getForTerminalsUtcDate(port: PortCode): (Seq[Terminal], UtcDate) => DBIOAction[Seq[ApiFlightWithSplits], NoStream, Effect.Read] =
    (terminals, date) =>
      table
        .filter(f => f.port === port.iata && f.terminal.inSet(terminals.map(_.toString)) && f.scheduledDateUtc === date.toISOString)
        .result
        .map(_.map(FlightSerialiser.fromRow))

  def getForUtcDate(port: PortCode): UtcDate => DBIOAction[Seq[ApiFlightWithSplits], NoStream, Effect.Read] =
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

  def removeAllBefore: UtcDate => DBIOAction[Int, NoStream, Effect.Write] = date =>
    table
      .filter(_.scheduledDateUtc < date.toISOString)
      .delete
}
