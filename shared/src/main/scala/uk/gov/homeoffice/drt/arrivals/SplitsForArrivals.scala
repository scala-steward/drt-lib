package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.DataUpdates.FlightUpdates
import uk.gov.homeoffice.drt.arrivals.SplitsForArrivals.updateFlightWithSplits
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages
import uk.gov.homeoffice.drt.ports.{ApiFeedSource, FeedSource}


object SplitsForArrivals {
  val empty: SplitsForArrivals = SplitsForArrivals(Map())

  def updateSplits(existing: Set[Splits], incoming: Set[Splits]): Set[Splits] =
    (existing.map(s => (s.source, s)).toMap ++ incoming.map(s => (s.source, s)).toMap).values.toSet

  def updateFlightWithSplits(flightWithSplits: ApiFlightWithSplits, splits: Set[Splits], nowMillis: Long): ApiFlightWithSplits = {
    val updatedArrival = splits.find(_.source == ApiSplitsWithHistoricalEGateAndFTPercentages) match {
      case None =>
        flightWithSplits.apiFlight
      case Some(liveSplit) =>
        val totalPax: Int = Math.round(liveSplit.totalPax)
        val transPax: Int = Math.round(liveSplit.totalPax - liveSplit.totalExcludingTransferPax).toInt
        val sources = flightWithSplits.apiFlight.FeedSources + ApiFeedSource
        val totalPaxSources = flightWithSplits.apiFlight.PassengerSources.updated(ApiFeedSource, Passengers(Some(totalPax), Option(transPax)))
        flightWithSplits.apiFlight.copy(
          FeedSources = sources,
          PassengerSources = totalPaxSources
        )
    }
    val updatedSplits = updateSplits(flightWithSplits.splits, splits)
    val updatedFlightWithSplits = flightWithSplits.copy(
      apiFlight = updatedArrival,
      splits = updatedSplits,
      lastUpdated = Option(nowMillis),
    )
    updatedFlightWithSplits
  }

}

case class SplitsForArrivals(splits: Map[UniqueArrival, Set[Splits]]) extends FlightUpdates {

  def diff(other: Map[UniqueArrival, Set[Splits]]): SplitsForArrivals = {
    val updatedSplits = splits
      .map {
        case (key, ourSplits) =>
          other.get(key)
            .map(existing => ourSplits.diff(existing))
            .collect {
              case ourNewSplits if ourNewSplits.nonEmpty =>
                (key, ourNewSplits)
            }
      }
      .collect { case Some(splits) => splits }
      .toMap

    SplitsForArrivals(updatedSplits)
  }

  def applyTo(flightsWithSplits: FlightsWithSplits, nowMillis: Long, sourceOrderPreference: List[FeedSource]): (FlightsWithSplits, Set[Long]) = {
    val minutesFromUpdates = splits.keys.flatMap { key =>
      flightsWithSplits.flights.get(key) match {
        case Some(fws) => fws.apiFlight.pcpRange(sourceOrderPreference)
        case None => Iterable()
      }
    }.toSet
    val updatedFlights = splits.foldLeft(flightsWithSplits.flights) {
      case (acc, (key, incoming)) =>
        acc.get(key) match {
          case Some(flightWithSplits) =>
            val updatedFlightWithSplits = updateFlightWithSplits(flightWithSplits, incoming, nowMillis)
            acc + (key -> updatedFlightWithSplits)
          case None => acc
        }
    }
    (FlightsWithSplits(updatedFlights), minutesFromUpdates)
  }


  def ++(tuple: (UniqueArrival, Set[Splits])): Map[UniqueArrival, Set[Splits]] = splits + tuple
}
