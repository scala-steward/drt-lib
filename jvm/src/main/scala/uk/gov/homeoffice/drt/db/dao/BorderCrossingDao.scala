package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.tables.{BorderCrossingRow, BorderCrossingTable, GateType}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, UtcDate}

import scala.concurrent.ExecutionContext


object BorderCrossingDao {
  val table: TableQuery[BorderCrossingTable] = TableQuery[BorderCrossingTable]

  def replaceHours(port: PortCode)
                  (implicit ec: ExecutionContext): (Terminal, GateType, Iterable[BorderCrossingRow]) => DBIOAction[Int, NoStream, Effect.Write] =
    (terminal, gateType, rows) => {
      val inserts = rows
        .filter { r =>
          r.portCode == port.iata &&
            r.terminal == terminal.toString &&
            r.gateType == gateType.value
        }
        .map(table.insertOrUpdate)

      DBIO.sequence(inserts).map(_.sum)
    }

  def get(portCode: String, terminal: String, date: String): DBIOAction[Seq[BorderCrossingRow], NoStream, Effect.Read] =
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
                val totalCapacity = rows.map(_.passengers).sum
                hourMillis -> totalCapacity
            }
        }

  private def filterLocalDate(rows: Seq[BorderCrossingRow], localDate: LocalDate): Seq[BorderCrossingRow] =
    rows.filter { row =>
      val utcDate = UtcDate.parse(row.dateUtc).getOrElse(throw new Exception(s"Failed to parse UtcDate from ${row.dateUtc}"))
      val rowLocalDate = SDate(utcDate).addHours(row.hour).toLocalDate
      rowLocalDate == localDate
    }

  private def filterPortTerminalDate(port: String, maybeTerminal: Option[String], localDate: LocalDate)
                                    (implicit ec: ExecutionContext): DBIOAction[Seq[BorderCrossingRow], NoStream, Effect.Read] = {
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

  def removeAllBefore: UtcDate => DBIOAction[Int, NoStream, Effect.Write] = date =>
    table
      .filter(_.dateUtc < date.toISOString)
      .delete
}
