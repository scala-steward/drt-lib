package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.tables.EgateSimulationRow
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{SDate, SDateLike, UtcDate}

import java.sql.Timestamp

case class EgateSimulationRequest(startDate: UtcDate,
                                  endDate: UtcDate,
                                  terminal: Terminal,
                                  uptakePercentage: Double,
                                  parentChildRatio: Double,
                                 )

case class EgateSimulation(uuid: String,
                           request: EgateSimulationRequest,
                           status: String,
                           createdAt: SDateLike,
                          )


object EgateSimulationSerialisation {
  def apply(row: EgateSimulationRow): EgateSimulation =
    EgateSimulation(
      uuid = row.uuid,
      EgateSimulationRequest(
        startDate = SDate(row.startDate.getTime).toUtcDate,
        endDate = SDate(row.endDate.getTime).toUtcDate,
        terminal = Terminal(row.terminal),
        uptakePercentage = row.uptakePercentage,
        parentChildRatio = row.parentChildRatio,
      ),
      status = row.status,
      createdAt = SDate(row.createdAt.getTime),
    )

  def apply(simulation: EgateSimulation): EgateSimulationRow =
    EgateSimulationRow(
      uuid = simulation.uuid,
      startDate = new Timestamp(SDate(simulation.request.startDate).millisSinceEpoch),
      endDate = new Timestamp(SDate(simulation.request.endDate).millisSinceEpoch),
      terminal = simulation.request.terminal.toString,
      uptakePercentage = simulation.request.uptakePercentage,
      parentChildRatio = simulation.request.parentChildRatio,
      status = simulation.status,
      createdAt = new Timestamp(simulation.createdAt.millisSinceEpoch),
    )
}
