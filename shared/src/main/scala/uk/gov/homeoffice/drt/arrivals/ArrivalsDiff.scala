package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.DataUpdates.FlightUpdates
import uk.gov.homeoffice.drt.ports.FeedSource
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import upickle.default.{macroRW, _}


object ArrivalsDiff {
  implicit val rw: ReadWriter[ArrivalsDiff] = macroRW

  val empty: ArrivalsDiff = ArrivalsDiff(Seq(), Seq())

  def apply(toUpdate: Iterable[Arrival], toRemove: Iterable[UniqueArrival]): ArrivalsDiff = ArrivalsDiff(
    toUpdate.map(a => (a.unique, a)).toMap, toRemove
  )
}

case class ArrivalsDiff(toUpdate: Map[UniqueArrival, Arrival], toRemove: Iterable[UniqueArrival]) extends FlightUpdates {
  def diff(arrivals: Map[UniqueArrival, Arrival]): ArrivalsDiff = {
    val updatedFlights = toUpdate
      .map {
        case (key, incomingArrival) =>
          arrivals.get(key) match {
            case Some(existingArrival) =>
              val updatedArrival = existingArrival.update(incomingArrival)
              if (updatedArrival.isEqualTo(existingArrival))
                None
              else
                Some(updatedArrival)
            case None =>
              Some(incomingArrival)
          }
      }
      .collect { case Some(updatedFlight) => updatedFlight }

    val validRemovals = toRemove.filter(toRemove => arrivals.contains(toRemove))

    ArrivalsDiff(updatedFlights, validRemovals)
  }

  def forTerminal(terminal: Terminal): ArrivalsDiff = ArrivalsDiff(
    toUpdate.filter { case (_, arrival) => arrival.Terminal == terminal },
    toRemove.filter(_.terminal == terminal)
  )

  def window(from: Long, to: Long): ArrivalsDiff = ArrivalsDiff(
    toUpdate.filter {
      case (_, arrival) =>
        from <= arrival.Scheduled && arrival.Scheduled <= to
    },
    toRemove.filter {
      arrival =>
        from <= arrival.scheduled && arrival.scheduled <= to
    }
  )

  def updateMinutes(sourceOrderPreference: List[FeedSource]): Set[Long] = toUpdate.values.flatMap(_.pcpRange(sourceOrderPreference)).toSet

  def applyTo(existingFlights: FlightsWithSplits, nowMillis: Long, sourceOrderPreference: List[FeedSource]): (FlightsWithSplits, Set[Long]) = {
    val updated = toUpdate.foldLeft(existingFlights.flights) {
      case (acc, (key, incomingArrival)) =>
        acc.get(key) match {
          case Some(existing) =>
//            val (feedSources, paxSources) = fws.splits.foldLeft((arrival.FeedSources, arrival.PassengerSources)) {
//              case ((accFs, accPs), split) if Set(ApiSplitsWithHistoricalEGateAndFTPercentages, Historical).contains(split.source) =>
//                val totalPax = Option(split.totalPax)
//                val transPax = if (split.transPax > 0) Option(split.transPax) else None
//                val fs = if (split.source == ApiSplitsWithHistoricalEGateAndFTPercentages) ApiFeedSource else HistoricApiFeedSource
//                (accFs + fs, accPs + (fs -> Passengers(totalPax, transPax)))
//              case ((accFs, accPs), _) => (accFs, accPs)
//            }
            val arrivalWithApiSources = incomingArrival.copy(
              FeedSources = existing.apiFlight.FeedSources ++ incomingArrival.FeedSources,
              PassengerSources = incomingArrival.PassengerSources.foldLeft(existing.apiFlight.PassengerSources) {
                case (acc, (key, updated)) => acc.updated(key, updated)
              },
            )
            acc + (key -> existing.copy(apiFlight = arrivalWithApiSources, lastUpdated = Option(nowMillis)))
          case None =>
            acc + (key -> ApiFlightWithSplits(incomingArrival, Set(), Option(nowMillis)))
        }
    }

    val minusRemovals: Map[UniqueArrival, ApiFlightWithSplits] = ArrivalsRemoval.removeArrivals(toRemove, updated)

    val minutesFromRemovalsInExistingState: Set[Long] = toRemove
      .flatMap { r => existingFlights.flights.get(r).map(_.apiFlight.pcpRange(sourceOrderPreference)).getOrElse(List()) }
      .toSet

    val minutesFromExistingStateUpdatedFlights = toUpdate
      .flatMap { case (unique, _) =>
        existingFlights.flights.get(unique) match {
          case None => Set()
          case Some(f) => f.apiFlight.pcpRange(sourceOrderPreference)
        }
      }.toSet

    val updatedMinutesFromFlights = minutesFromRemovalsInExistingState ++
      updateMinutes(sourceOrderPreference) ++
      minutesFromExistingStateUpdatedFlights

    (FlightsWithSplits(minusRemovals), updatedMinutesFromFlights)
  }
}
