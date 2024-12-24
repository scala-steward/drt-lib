package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.DataUpdates.FlightUpdates
import uk.gov.homeoffice.drt.arrivals.SplitsForArrivals.updateFlightWithSplits
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages
import uk.gov.homeoffice.drt.ports.{ApiFeedSource, FeedSource, PortCode}
import upickle.default.{macroRW, _}


object SplitsForArrivals {
  implicit val rw: ReadWriter[SplitsForArrivals] = macroRW

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

  def diff(oldOnes: Map[UniqueArrival, Set[Splits]]): SplitsForArrivals = {
    val updatedSplits = splits
      .map { case (ua, split) =>
        val oldSplits = oldOnes.getOrElse(ua, Set())
        split.diff(oldSplits) match {
          case empty if empty.isEmpty => None
          case newSplits => Option((ua, newSplits))
        }
      }
      .collect {
        case Some(updates) => updates
      }
      .toMap

    SplitsForArrivals(updatedSplits)
  }

  def applyTo(flightsWithSplits: FlightsWithSplits,
              nowMillis: Long,
              sourceOrderPreference: List[FeedSource],
             ): (FlightsWithSplits, Set[Long], Iterable[ApiFlightWithSplits], Iterable[UniqueArrival]) = {
    val minutesFromUpdates = splits.flatMap {
      case (key, splits) =>
        flightsWithSplits.flights.get(key) match {
          case Some(fws) if splits.exists(_.source == ApiSplitsWithHistoricalEGateAndFTPercentages) => fws.apiFlight.pcpRange(sourceOrderPreference)
          case _ =>
            findWithPreviousPort(flightsWithSplits, key)
              .map(_.apiFlight.pcpRange(sourceOrderPreference))
              .getOrElse(Iterable.empty)
        }
    }.toSet

    val updatedFlights = splits
      .map {
        case (key, incomingSplits) =>
          flightsWithSplits.flights.get(key) match {
            case Some(existingFws) =>
              val updatedFlightWithSplits = updateFlightWithSplits(existingFws, incomingSplits, nowMillis)
              Option(updatedFlightWithSplits)
            case None =>
              findWithPreviousPort(flightsWithSplits, key)
                .map(updateFlightWithSplits(_, incomingSplits, nowMillis))
          }
      }
      .collect { case Some(fws) => fws }

    val updated = flightsWithSplits.flights ++ updatedFlights.map(f => f.unique -> f)

    (FlightsWithSplits(updated), minutesFromUpdates, updatedFlights, Seq.empty)
  }

  private def findWithPreviousPort(flightsWithSplits: FlightsWithSplits, key: UniqueArrival): Option[ApiFlightWithSplits] =
    flightsWithSplits.flights
      .collect {
        case (_, fws) if fws.apiFlight.PreviousPort.nonEmpty => fws
      }
      .find { fws =>
        key == UniqueArrival(fws.apiFlight.VoyageNumber.numeric, fws.apiFlight.Terminal, fws.apiFlight.Scheduled, fws.apiFlight.PreviousPort.getOrElse(PortCode("")))
      }

  def ++(tuple: (UniqueArrival, Set[Splits])): Map[UniqueArrival, Set[Splits]] = splits + tuple
}
