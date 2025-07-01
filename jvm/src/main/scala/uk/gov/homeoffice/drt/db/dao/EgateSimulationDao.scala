package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.{EgateSimulation, EgateSimulationRequest, EgateSimulationSerialisation}
import uk.gov.homeoffice.drt.db.tables.EgateSimulationTable
import uk.gov.homeoffice.drt.time.SDate

import java.sql.Timestamp
import scala.concurrent.ExecutionContext


case class EgateSimulationDao()
                             (implicit ec: ExecutionContext) {
  val table: TableQuery[EgateSimulationTable] = TableQuery[EgateSimulationTable]

  def get(uuid: String): DBIOAction[Option[EgateSimulation], NoStream, Effect.Read] =
    table
      .filter(_.uuid === uuid)
      .result
      .map(_.map(r => EgateSimulationSerialisation(r)).headOption)

  def get(request: EgateSimulationRequest): DBIOAction[Option[EgateSimulation], NoStream, Effect.Read] =
    table
      .filter(r =>
        r.startDate === new Timestamp(SDate(request.startDate).millisSinceEpoch) &&
          r.endDate === new Timestamp(SDate(request.endDate).millisSinceEpoch) &&
          r.terminal === request.terminal.toString &&
          r.uptakePercentage === request.uptakePercentage &&
          r.parentChildRatio === request.parentChildRatio
      )
      .result
      .map(_.map(r => EgateSimulationSerialisation(r)).headOption)

  def insertOrUpdate(simulation: EgateSimulation): DBIOAction[Int, NoStream, Effect.Write with Effect.Transactional] =
    table.insertOrUpdate(EgateSimulationSerialisation(simulation))
}
