package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import slick.sql.FixedSqlAction
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.{CapacityHourlyRow, CapacityHourlyTable}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, UtcDate}

import scala.concurrent.ExecutionContext


object CapacityHourlyDao {
  val table: TableQuery[CapacityHourlyTable] = TableQuery[CapacityHourlyTable]

  def replaceHours(port: PortCode): (Terminal, Iterable[CapacityHourlyRow]) => DBIOAction[Unit, NoStream, Effect.Write with Effect.Transactional] =
    (terminal, rows) => {
      val validRows = rows.filter(r => r.portCode == port.iata && r.terminal == terminal.toString).toSet

      if (validRows.nonEmpty) {
        val dateHours = validRows.map {
          case CapacityHourlyRow(_, _, dateUtc, hour, _, _) => (dateUtc, hour)
        }

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

        val insertAction = table ++= validRows

        DBIO.seq(deleteAction, insertAction).transactionally
      }
      else DBIO.successful()
    }

  def get(portCode: String, terminal: String, date: String): DBIOAction[Seq[CapacityHourlyRow], NoStream, Effect.Read] =
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
          _.capacity
        }.sum)

  def hourlyForPortAndDate(port: String, maybeTerminal: Option[String])
                          (implicit ec: ExecutionContext): LocalDate => DBIOAction[Map[Long, Int], NoStream, Effect.Read] =
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
                val totalCapacity = rows.map(_.capacity).sum
                hourMillis -> totalCapacity
            }
        }

  private def filterLocalDate(rows: Seq[CapacityHourlyRow], localDate: LocalDate): Seq[CapacityHourlyRow] =
    rows.filter { row =>
      val utcDate = UtcDate.parse(row.dateUtc).getOrElse(throw new Exception(s"Failed to parse UtcDate from ${row.dateUtc}"))
      val rowLocalDate = SDate(utcDate).addHours(row.hour).toLocalDate
      rowLocalDate == localDate
    }

  private def filterPortTerminalDate(port: String, maybeTerminal: Option[String], localDate: LocalDate)
                                    (implicit ec: ExecutionContext): DBIOAction[Seq[CapacityHourlyRow], NoStream, Effect.Read] = {
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
