package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.{EgateEligibility, EgateEligibilitySerialisation}
import uk.gov.homeoffice.drt.db.tables.EgateEligibilityTable
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import scala.concurrent.ExecutionContext


case class EgateEligibilityDao()
                              (implicit ec: ExecutionContext) {
  val table: TableQuery[EgateEligibilityTable] = TableQuery[EgateEligibilityTable]

  def get(port: PortCode, terminal: Terminal, date: UtcDate): DBIOAction[Option[EgateEligibility], NoStream, Effect.Read] =
    table
      .filter(r =>
        r.port === port.iata &&
          r.terminal === terminal.toString &&
          r.dateUtc === date.toISOString
      )
      .result
      .map(_.map(r => EgateEligibilitySerialisation(r)).headOption)

  def insertOrUpdate(eligibility: EgateEligibility): DBIOAction[Int, NoStream, Effect.Write with Effect.Transactional] =
    table.insertOrUpdate(EgateEligibilitySerialisation(eligibility))
}
