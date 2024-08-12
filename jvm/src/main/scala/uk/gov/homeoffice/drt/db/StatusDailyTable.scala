package uk.gov.homeoffice.drt.db

import slick.lifted.Tag
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.LocalDate

import java.sql.Timestamp

case class StatusDaily(portCode: PortCode,
                       terminal: Terminal,
                       dateLocal: LocalDate,
                       paxLoadsUpdatedAt: Option[Long],
                       deskRecommendationsUpdatedAt: Option[Long],
                       staffDeploymentsUpdatedAt: Option[Long],
                       staffUpdatedAt: Option[Long],
                      )

case class StatusDailyRow(portCode: String,
                          terminal: String,
                          dateLocal: String,
                          paxLoadsUpdatedAt: Option[Timestamp],
                          deskRecommendationsUpdatedAt: Option[Timestamp],
                          staffDeploymentsUpdatedAt: Option[Timestamp],
                          staffUpdatedAt: Option[Timestamp],
                         )

class StatusDailyTable(tag: Tag)
  extends Table[StatusDailyRow](tag, "status_daily") {

  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def dateLocal: Rep[String] = column[String]("date_local")

  def paxLoadsUpdatedAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("pax_loads_updated_at")

  def deskRecommendationsUpdatedAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("desk_recommendations_updated_at")

  def staffDeploymentsUpdatedAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("staff_deployments_updated_at")

  def staffUpdatedAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("staff_updated_at")

  def pk = primaryKey("pk_status_daily_port_terminal_queue_datelocal_hour", (port, terminal, dateLocal))

  def * = (
    port,
    terminal,
    dateLocal,
    paxLoadsUpdatedAt,
    deskRecommendationsUpdatedAt,
    staffDeploymentsUpdatedAt,
    staffUpdatedAt
  ) <> (StatusDailyRow.tupled, StatusDailyRow.unapply)
}


