package uk.gov.homeoffice.drt.db

import slick.lifted.Tag
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, UtcDate}

import java.sql.Timestamp
import scala.concurrent.ExecutionContext

case class PassengersHourlyRow(port: String,
                               terminal: String,
                               queue: String,
                               dateUtc: UtcDate,
                               hour: Int,
                               passengers: Int,
                               lastUpdated: Option[Timestamp])


class PassengersHourlyTable(tag: Tag)
  extends Table[(String, String, String, String, Int, Int, Timestamp)](tag, "passengers_hourly") {

  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def queue: Rep[String] = column[String]("queue")

  def dateUtc: Rep[String] = column[String]("date_utc")

  def hour: Rep[Int] = column[Int]("hour")

  def passengers: Rep[Int] = column[Int]("passengers")

  def updatedAt: Rep[Timestamp] = column[java.sql.Timestamp]("updated_at")

  def pk = primaryKey("pk_port_terminal_queue_dateutc_hour", (port, terminal, queue, dateUtc, hour))

  def * = (port, terminal, queue, dateUtc, hour, passengers, updatedAt)
}

object PassengersHourlyQueries {
  val table: TableQuery[PassengersHourlyTable] = TableQuery[PassengersHourlyTable]

  def replaceHours(port: PortCode): (Terminal, Iterable[PassengersHourlyRow]) => DBIOAction[Int, NoStream, Effect.Write] =
    (terminal, rows) => {
      val dateHours = rows.map {
        case PassengersHourlyRow(_, _, _, dateUtc, hour, _, _) => (dateUtc.toISOString, hour)
      }.toSet

      table
        .filter(_.port === port.iata.toLowerCase)
        .filter(_.terminal === terminal.toString.toLowerCase)
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
        .andThen {
          table ++= rows.map {
            case PassengersHourlyRow(port, terminal, queue, dateUtc, hour, passengers, _) =>
              (port, terminal, queue, dateUtc.toISOString, hour, passengers, new Timestamp(SDate.now().millisSinceEpoch))
          }
        }
        .transactionally
    }

  def totalForPortAndDate(port: String)
                         (implicit ec: ExecutionContext): LocalDate => DBIOAction[Int, NoStream, Effect.Read] =
    localDate => {
      val sdate = SDate(localDate)
      val utcDates = Set(
        sdate.getLocalLastMidnight.toUtcDate,
        sdate.getLocalNextMidnight.toUtcDate,
      )

      table
        .filter(_.port === port.toLowerCase)
        .filter(_.dateUtc inSet utcDates.map(_.toISOString))
        .result
        .map { rows =>
          rows.filter {
            case (_, _, _, utc, hour, _, _) =>
              val Array(y, m, d) = utc.split("-").map(_.toInt)
              val rowLocalDate = SDate(UtcDate(y, m, d)).addHours(hour).toLocalDate
              rowLocalDate == localDate
          }.map(_._5).sum
        }
    }
}
