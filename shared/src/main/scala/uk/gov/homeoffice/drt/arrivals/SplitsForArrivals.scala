package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.DataUpdates.FlightUpdates
import uk.gov.homeoffice.drt.ports.FeedSource


object SplitsForArrivals {
  val empty: SplitsForArrivals = SplitsForArrivals(Map())
}

case class SplitsForArrivals(splits: Map[UniqueArrival, Set[Splits]]) extends FlightUpdates {

  private def updateSplits(existing: Set[Splits], incoming: Set[Splits]): Set[Splits] =
    (existing.map(s => (s.source, s)).toMap ++ incoming.map(s => (s.source, s)).toMap).values.toSet

  def diff(other: Map[UniqueArrival, Set[Splits]]): SplitsForArrivals = {
    val updatedSplits = other
      .map {
        case (key, incoming) =>
          println(s"looking for $key to update splits. keys: ${splits.keys.map(_.toString).mkString(", ")}")
          other.get(key)
            .map { existing =>
              println(s"found $key to update splits. existing: ${existing.map(_.toString).mkString(", ")} incoming: ${incoming.map(_.toString).mkString(", ")}")
              val newIncoming = incoming.diff(existing)
              println(s"new incoming: ${newIncoming.map(_.toString).mkString(", ")}")
              (existing, newIncoming)
            }
            .collect {
              case (existing, incoming) if incoming.nonEmpty =>
                (key, updateSplits(existing, incoming))
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
        println(s"looking for $key to update splits. keys: ${acc.keys.map(_.toString).mkString(", ")}")
        acc.get(key) match {
          case Some(flightWithSplits) =>
            val updatedSplits = updateSplits(flightWithSplits.splits, incoming)
            val updatedFlightWithSplits = flightWithSplits.copy(splits = updatedSplits, lastUpdated = Option(nowMillis))
            acc + (key -> updatedFlightWithSplits)
          case None => acc
        }
    }
    (FlightsWithSplits(updatedFlights), minutesFromUpdates)
  }

  def ++(tuple: (UniqueArrival, Set[Splits])): Map[UniqueArrival, Set[Splits]] = splits + tuple
}
