package uk.gov.homeoffice.drt.services.exports

//import actors.PartitionedPortStateActor.{FlightsRequest, GetFlightsForTerminals}
import uk.gov.homeoffice.drt.arrivals.ApiFlightWithSplits
import uk.gov.homeoffice.drt.ports.FeedSource
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.LocalDate

trait FlightsWithSplitsWithoutActualApiExport extends FlightsWithSplitsExport {
//  val request: FlightsRequest = GetFlightsForTerminals(SDate(start).millisSinceEpoch, SDate(end).addDays(1).addMinutes(-1).millisSinceEpoch, terminals)
}

case class FlightsWithSplitsWithoutActualApiExportImpl(start: LocalDate,
                                                       end: LocalDate,
                                                       terminals: Seq[Terminal],
                                                       paxFeedSourceOrder: List[FeedSource],
                                                      ) extends FlightsWithSplitsWithoutActualApiExport {
  override val flightsFilter: (ApiFlightWithSplits, Seq[Terminal]) => Boolean = standardFilter
}
