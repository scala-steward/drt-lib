package uk.gov.homeoffice.drt.db.queries

import slick.dbio.Effect
import slick.sql.FixedSqlAction
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.{PassengersHourly, PassengersHourlyRow, PassengersHourlyTable}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, UtcDate}

import java.sql.Timestamp
import scala.concurrent.ExecutionContext

object PassengersHourlySerialiser {
  val toRow: PassengersHourly => PassengersHourlyRow = {
    case PassengersHourly(portCode, terminal, queue, dateUtc, hour, passengers, _) =>
      PassengersHourlyRow(
        portCode.iata,
        terminal.toString,
        queue.toString,
        dateUtc.toISOString,
        hour,
        passengers,
        new Timestamp(SDate.now().millisSinceEpoch),
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
        None,
      )
  }
}

object PassengersHourlyQueries {
  val table: TableQuery[PassengersHourlyTable] = TableQuery[PassengersHourlyTable]

  def replaceHours(port: PortCode)
                  (implicit ec: ExecutionContext): (Terminal, Iterable[PassengersHourlyRow]) =>
    DBIOAction[Option[Int], NoStream, Effect.Write with Effect.Transactional] =
    (terminal, rows) => {
      val dateHours = rows.map {
        case PassengersHourlyRow(_, _, _, dateUtc, hour, _, _) => (dateUtc, hour)
      }.toSet

      val delete: FixedSqlAction[Int, NoStream, Effect.Write] = table
        .filter(_.port === port.iata)
        .filter(_.terminal === terminal.toString)
        .filter { row =>
          dateHours
            .map {
              case (date, hour) =>
                val value: Rep[Boolean] = row.dateUtc === date && row.hour === hour
                value
            }
            .reduce(_ || _)
        }
        .delete
      val insert: FixedSqlAction[Option[Int], NoStream, Effect.Write] = table ++= rows.map {
        case PassengersHourlyRow(port, terminal, queue, dateUtc, hour, passengers, _) =>
          (port, terminal, queue, dateUtc, hour, passengers, new Timestamp(SDate.now().millisSinceEpoch))
      }
      val transaction = (for {
        _ <- delete
        v <- insert
      } yield v).transactionally

      transaction
    }

  def get(portCode: String, terminal: String, date: String)
         (implicit ec: ExecutionContext): DBIOAction[Seq[PassengersHourlyRow], NoStream, Effect.Read] =
    table
      .filter(_.port === portCode)
      .filter(_.terminal === terminal)
      .filter(_.dateUtc === date)
      .result
      .map(_.map(PassengersHourlyRow.tupled))

  def totalForPortAndDate(port: String, maybeTerminal: Option[String])
                         (implicit ec: ExecutionContext): LocalDate => DBIOAction[Int, NoStream, Effect.Read] =
    localDate => {
      filerPortTerminalDate(port, maybeTerminal, localDate)
        .map(_.map(_._6).sum)
    }

  def hourlyForPortAndDate(port: String, maybeTerminal: Option[String])
                          (implicit ec: ExecutionContext): LocalDate => DBIOAction[Map[(UtcDate, Int), Int], NoStream, Effect.Read] =
    localDate => {
      filerPortTerminalDate(port, maybeTerminal, localDate)
        .map {
          _
            .groupBy(r => (r._4, r._5))
            .map {
              case ((date, hour), rows) =>
                val utcDate = UtcDate.parse(date).getOrElse(throw new Exception(s"Failed to parse UtcDate from $date"))
                (utcDate, hour) -> rows.map(_._6).sum
            }
        }
    }

  private def filterLocalDate(rows: Seq[(String, String, String, String, Int, Int, Timestamp)], localDate: LocalDate): Seq[(String, String, String, String, Int, Int, Timestamp)] =
    rows.filter {
      case (_, _, _, utc, hour, _, _) =>
        val utcDate = UtcDate.parse(utc).getOrElse(throw new Exception(s"Failed to parse UtcDate from $utc"))
        val rowLocalDate = SDate(utcDate).addHours(hour).toLocalDate
        rowLocalDate == localDate
    }

  private def filerPortTerminalDate(port: String, maybeTerminal: Option[String], localDate: LocalDate)
                                   (implicit ec: ExecutionContext): DBIOAction[Seq[(String, String, String, String, Int, Int, Timestamp)], NoStream, Effect.Read] = {
    val sdate = SDate(localDate)
    val utcDates = Set(
      sdate.getLocalLastMidnight.toUtcDate,
      sdate.getLocalNextMidnight.toUtcDate,
    )

    table
      .filter { row =>
        val portMatches = row.port === port
        val terminalMatches = maybeTerminal.fold(true.bind)(terminal => row.terminal === terminal)
        portMatches && terminalMatches
      }
      .filter(_.dateUtc inSet utcDates.map(_.toISOString))
      .result
      .map(rows => filterLocalDate(rows, localDate))
  }
}
