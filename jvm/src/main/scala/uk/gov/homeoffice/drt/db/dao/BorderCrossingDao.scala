package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import uk.gov.homeoffice.drt.db.tables.{BorderCrossingRow, BorderCrossingTable, GateType, GateTypes}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{PortCode, Queues}
import uk.gov.homeoffice.drt.time.{DateLike, LocalDate, SDate, UtcDate}

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
                         (implicit ec: ExecutionContext): DateLike => DBIOAction[Int, NoStream, Effect.Read] =
    date =>
      filterPortTerminalDate(port, maybeTerminal, date)
        .map(_.map(_.passengers).sum)

  def queueTotalsForPortAndDate(port: String, maybeTerminal: Option[String])
                               (implicit ec: ExecutionContext): DateLike => DBIOAction[Map[Queue, Int], NoStream, Effect.Read] =
    date => filterPortTerminalDate(port, maybeTerminal, date).map(rowsToQueueTotals)

  private def rowsToQueueTotals(rows: Seq[BorderCrossingRow]): Map[Queue, Int] =
    rows
      .groupBy(_.gateType)
      .map {
        case (gateType, queueRows) =>
          (queueForGateType(gateType), queueRows.map(_.passengers).sum)
      }

  def hourlyForPortAndDate(port: String, maybeTerminal: Option[String])
                          (implicit ec: ExecutionContext): DateLike => DBIOAction[Map[Long, Map[Queue, Int]], NoStream, Effect.Read] =
    date =>
      filterPortTerminalDate(port, maybeTerminal, date)
        .map {
          _
            .groupBy { r =>
              (r.dateUtc, r.hour)
            }
            .map {
              case ((date, hour), rows) =>
                val utcDate = UtcDate.parse(date).getOrElse(throw new Exception(s"Failed to parse UtcDate from $date"))
                val hourMillis = SDate(utcDate).addHours(hour).millisSinceEpoch
                val byQueue = rows.groupBy(_.gateType).map {
                  case (gateType, queueRows) =>
                    val queue: Queue = queueForGateType(gateType)
                    val queueTotal = queueRows.map(_.passengers).sum
                    queue -> queueTotal
                }
                hourMillis -> byQueue
            }
        }

  private def queueForGateType(gateType: String): Queue =
    gateType match {
      case GateTypes.EGate.value => Queues.EGate
      case GateTypes.Pcp.value => Queues.QueueDesk
    }


  def filterDate(rows: Seq[BorderCrossingRow], date: DateLike): Seq[BorderCrossingRow] =
    rows.filter { row =>
      val rowUtcDate = UtcDate.parse(row.dateUtc).getOrElse(throw new Exception(s"Failed to parse UtcDate from ${row.dateUtc}"))
      date match {
        case localDate: LocalDate =>
          SDate(rowUtcDate).addHours(row.hour).toLocalDate == localDate
        case utcDate: UtcDate =>
          SDate(rowUtcDate).addHours(row.hour).toUtcDate == utcDate
      }
    }

  private def filterPortTerminalDate(port: String, maybeTerminal: Option[String], date: DateLike)
                                    (implicit ec: ExecutionContext): DBIOAction[Seq[BorderCrossingRow], NoStream, Effect.Read] = {
    val sdate = SDate(date)
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
      .map(rows => filterDate(rows, date))
  }

  def removeAllBefore: UtcDate => DBIOAction[Int, NoStream, Effect.Write] = date =>
    table
      .filter(_.dateUtc < date.toISOString)
      .delete
}
