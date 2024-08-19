package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import slick.sql.FixedSqlAction
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.tables.{PassengersHourlyRow, PassengersHourlyTable}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, UtcDate}

import scala.concurrent.ExecutionContext


object PassengersHourlyDao {
  val table: TableQuery[PassengersHourlyTable] = TableQuery[PassengersHourlyTable]

  def replaceHours(port: PortCode): (Terminal, Iterable[PassengersHourlyRow]) => DBIOAction[Unit, NoStream, Effect.Write with Effect.Transactional] =
    (terminal, rows) => {
      val validRows = rows.filter(r => r.portCode == port.iata && r.terminal == terminal.toString)

      if (validRows.nonEmpty) {
        val dateHours = validRows.map {
          case PassengersHourlyRow(_, _, _, dateUtc, hour, _, _) => (dateUtc, hour)
        }.toSet

        val deleteAction: FixedSqlAction[Int, NoStream, Effect.Write] = table
          .filter(_.port === port.iata)
          .filter(_.terminal === terminal.toString)
          .filter { row =>
            dateHours
              .map {
                case (date, hour) => row.dateUtc === date && row.hour === hour
              }
              .reduce(_ || _)
          }
          .delete
        val insertAction: FixedSqlAction[Option[Int], NoStream, Effect.Write] = table ++= validRows

        DBIO.seq(deleteAction, insertAction).transactionally
      }
      else DBIO.successful()
    }

  def get(portCode: String, terminal: String, date: String): DBIOAction[Seq[PassengersHourlyRow], NoStream, Effect.Read] =
    table
      .filter(_.port === portCode)
      .filter(_.terminal === terminal)
      .filter(_.dateUtc === date)
      .result

  def totalForPortAndDate(port: String, maybeTerminal: Option[String])
                         (implicit ec: ExecutionContext): LocalDate => DBIOAction[Int, NoStream, Effect.Read] =
    localDate =>
      filterPortTerminalDate(port, maybeTerminal, localDate)
        .map(_.map {
          _.passengers
        }.sum)

  def queueTotalsForPortAndDate(port: String, maybeTerminal: Option[String])
                               (implicit ec: ExecutionContext): LocalDate => DBIOAction[Map[Queue, Int], NoStream, Effect.Read] =
    localDate => filterPortTerminalDate(port, maybeTerminal, localDate).map(rowsToQueueTotals)

  private def rowsToQueueTotals(rows: Seq[PassengersHourlyRow]): Map[Queue, Int] =
    rows
      .groupBy(_.queue)
      .map {
        case (queue, queueRows) =>
          (Queue(queue), queueRows.map(_.passengers).sum)
      }

  def hourlyForPortAndDate(port: String, maybeTerminal: Option[String])
                          (implicit ec: ExecutionContext): LocalDate => DBIOAction[Map[Long, Map[Queue, Int]], NoStream, Effect.Read] =
    localDate =>
      filterPortTerminalDate(port, maybeTerminal, localDate)
        .map {
          _
            .groupBy { r =>
              (r.dateUtc, r.hour)
            }
            .map {
              case ((date, hour), rows) =>
                val utcDate = UtcDate.parse(date).getOrElse(throw new Exception(s"Failed to parse UtcDate from $date"))
                val hourMillis = SDate(utcDate).addHours(hour).millisSinceEpoch
                val byQueue = rows.groupBy(_.queue).map {
                  case (queue, queueRows) =>
                    val queueTotal = queueRows.map(_.passengers).sum
                    Queue(queue) -> queueTotal
                }
                hourMillis -> byQueue
            }
        }

  private def filterLocalDate(rows: Seq[PassengersHourlyRow], localDate: LocalDate): Seq[PassengersHourlyRow] =
    rows.filter { row =>
      val utcDate = UtcDate.parse(row.dateUtc).getOrElse(throw new Exception(s"Failed to parse UtcDate from ${row.dateUtc}"))
      val rowLocalDate = SDate(utcDate).addHours(row.hour).toLocalDate
      rowLocalDate == localDate
    }

  private def filterPortTerminalDate(port: String, maybeTerminal: Option[String], localDate: LocalDate)
                                    (implicit ec: ExecutionContext): DBIOAction[Seq[PassengersHourlyRow], NoStream, Effect.Read] = {
    val sdate = SDate(localDate)
    val startUtcDate = sdate.getLocalLastMidnight.toUtcDate
    val endUtcDate = sdate.getLocalNextMidnight.addMinutes(-1).toUtcDate
    val utcDates = Set(startUtcDate, endUtcDate)

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
