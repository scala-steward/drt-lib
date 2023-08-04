package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.DataUpdates.FlightUpdates
import uk.gov.homeoffice.drt.ports.ApiFeedSource
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages


object SplitsForArrivals {
  val empty: SplitsForArrivals = SplitsForArrivals(Map())
}

case class SplitsForArrivals(splits: Map[UniqueArrival, Set[Splits]]) extends FlightUpdates {
  def diff(flights: FlightsWithSplits, nowMillis: Long): FlightsWithSplitsDiff = {
    val updatedFlights = splits
      .map {
        case (key, newSplits) =>
          flights.flights.get(key)
            .map(fws => (fws, newSplits.diff(fws.splits)))
            .collect {
              case (fws, updatedSplits) if updatedSplits.nonEmpty =>
                val updatedSources = updatedSplits.map(_.source)
                val mergedSplits = fws.splits.filterNot(s => updatedSources.contains(s.source)) ++ updatedSplits
                val updatedArrival = mergedSplits.find(_.source == ApiSplitsWithHistoricalEGateAndFTPercentages) match {
                  case None =>
                    fws.apiFlight
                  case Some(liveSplit) =>
                    val totalPax: Int = Math.round(liveSplit.totalPax)
                    val transPax: Int = Math.round(liveSplit.totalPax - liveSplit.totalExcludingTransferPax).toInt
                    val sources = fws.apiFlight.FeedSources + ApiFeedSource
                    val totalPaxSources = fws.apiFlight.PassengerSources.updated(ApiFeedSource, Passengers(Some(totalPax), Option(transPax)))
                    fws.apiFlight.copy(
                      FeedSources = sources,
                      PassengerSources = totalPaxSources
                    )
                }

                fws.copy(apiFlight = updatedArrival, splits = mergedSplits, lastUpdated = Option(nowMillis))
            }
      }
      .collect { case Some(flight) => flight }

    FlightsWithSplitsDiff(updatedFlights, List())
  }

  def ++(tuple: (UniqueArrival, Set[Splits])): Map[UniqueArrival, Set[Splits]] = splits + tuple
}
