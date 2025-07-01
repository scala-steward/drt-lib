package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.tables.EgateSimulationRow
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{SDate, SDateLike, UtcDate}

import java.sql.Timestamp

case class EgateSimulationRequest(portCode: PortCode,
                                  terminal: Terminal,
                                  startDate: UtcDate,
                                  endDate: UtcDate,
                                  uptakePercentage: Double,
                                  parentChildRatio: Double,
                                 )

case class EgateSimulationResponse(csvContent: String,
                                   meanAbsolutePercentageError: Double,
                                   standardDeviation: Double,
                                   bias: Double,
                                   correlationCoefficient: Double,
                                   rSquaredError: Double,
                                  )

case class EgateSimulation(uuid: String,
                           request: EgateSimulationRequest,
                           status: String,
                           response: Option[EgateSimulationResponse],
                           createdAt: SDateLike,
                          )

object EgateSimulationSerialisation {
  def apply(row: EgateSimulationRow): EgateSimulation = {
    val maybeResponse = for {
      csvContent <- row.csvContent
      mape <- row.meanAbsolutePercentageError
      standardDeviation <- row.standardDeviation
      bias <- row.bias
      correlationCoefficient <- row.correlationCoefficient
      rSquaredError <- row.rSquaredError
    } yield EgateSimulationResponse(
      csvContent = csvContent,
      meanAbsolutePercentageError = mape,
      standardDeviation = standardDeviation,
      bias = bias,
      correlationCoefficient = correlationCoefficient,
      rSquaredError = rSquaredError,
    )

    EgateSimulation(
      uuid = row.uuid,
      EgateSimulationRequest(
        portCode = PortCode(row.port),
        terminal = Terminal(row.terminal),
        startDate = SDate(row.startDate.getTime).toUtcDate,
        endDate = SDate(row.endDate.getTime).toUtcDate,
        uptakePercentage = row.uptakePercentage,
        parentChildRatio = row.parentChildRatio,
      ),
      status = row.status,
      response = maybeResponse,
      createdAt = SDate(row.createdAt.getTime),
    )
  }

  def apply(simulation: EgateSimulation): EgateSimulationRow =
    EgateSimulationRow(
      uuid = simulation.uuid,
      port = simulation.request.portCode.iata,
      terminal = simulation.request.terminal.toString,
      startDate = new Timestamp(SDate(simulation.request.startDate).millisSinceEpoch),
      endDate = new Timestamp(SDate(simulation.request.endDate).millisSinceEpoch),
      uptakePercentage = simulation.request.uptakePercentage,
      parentChildRatio = simulation.request.parentChildRatio,
      status = simulation.status,
      csvContent = simulation.response.map(_.csvContent),
      meanAbsolutePercentageError = simulation.response.map(_.meanAbsolutePercentageError),
      standardDeviation = simulation.response.map(_.standardDeviation),
      bias = simulation.response.map(_.bias),
      correlationCoefficient = simulation.response.map(_.correlationCoefficient),
      rSquaredError = simulation.response.map(_.rSquaredError),
      createdAt = new Timestamp(simulation.createdAt.millisSinceEpoch),
    )
}
