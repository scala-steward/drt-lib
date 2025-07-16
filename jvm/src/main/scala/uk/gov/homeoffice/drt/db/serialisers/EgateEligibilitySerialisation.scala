package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.tables.EgateEligibilityRow
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{SDate, SDateLike, UtcDate}

import java.sql.Timestamp


case class EgateEligibility(port: PortCode,
                            terminal: Terminal,
                            dateUtc: UtcDate,
                            totalPassengers: Int,
                            egateEligiblePct: Double,
                            egateUnderAgePct: Double,
                            createdAt: SDateLike,
                          )

object EgateEligibilitySerialisation {
  def apply(row: EgateEligibilityRow): EgateEligibility =
    EgateEligibility(
      port = PortCode(row.port),
      terminal = Terminal(row.terminal),
      dateUtc = UtcDate.parse(row.dateUtc).getOrElse(throw new Exception(s"Failed to parse dateUtc: ${row.dateUtc}")),
      totalPassengers = row.totalPassengers,
      egateEligiblePct = row.egateEligiblePct,
      egateUnderAgePct = row.egateUnderAgePct,
      createdAt = SDate(row.createdAt.getTime)
    )

  def apply(simulation: EgateEligibility): EgateEligibilityRow =
    EgateEligibilityRow(
      port = simulation.port.iata,
      terminal = simulation.terminal.toString,
      dateUtc = simulation.dateUtc.toISOString,
      totalPassengers = simulation.totalPassengers,
      egateEligiblePct = simulation.egateEligiblePct,
      egateUnderAgePct = simulation.egateUnderAgePct,
      createdAt = new Timestamp(simulation.createdAt.millisSinceEpoch)
    )
}
