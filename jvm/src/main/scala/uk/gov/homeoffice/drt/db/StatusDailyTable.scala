package uk.gov.homeoffice.drt.db

import slick.lifted.Tag
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

case class StatusDaily(portCode: PortCode,
                       terminal: Terminal,
                       dateUtc: UtcDate,
                       paxLoadsUpdatedAt: Long,
                       deskRecommendationsUpdatedAt: Long,
                       deskDeploymentsUpdatedAt: Long,
                      )

case class StatusDailyRow(portCode: String,
                          terminal: String,
                          dateUtc: String,
                          paxLoadsUpdatedAt: Timestamp,
                          deskRecommendationsUpdatedAt: Timestamp,
                          deskDeploymentsUpdatedAt: Timestamp,
                         )

class StatusDailyTable(tag: Tag)
  extends Table[StatusDailyRow](tag, "status_daily") {

  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def dateUtc: Rep[String] = column[String]("date_utc")

  def paxLoadsUpdatedAt: Rep[Timestamp] = column[Timestamp]("pax_loads_updated_at")

  def deskRecommendationsUpdatedAt: Rep[Timestamp] = column[Timestamp]("desk_recommendations_updated_at")

  def deskDeploymentsUpdatedAt: Rep[Timestamp] = column[Timestamp]("desk_deployments_updated_at")

  def pk = primaryKey("pk_status_daily_port_terminal_queue_dateutc_hour", (port, terminal, dateUtc))

  def * = (
    port,
    terminal,
    dateUtc,
    paxLoadsUpdatedAt,
    deskRecommendationsUpdatedAt,
    deskDeploymentsUpdatedAt) <> (StatusDailyRow.tupled, StatusDailyRow.unapply)
}


