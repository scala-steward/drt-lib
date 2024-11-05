package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.DataUpdates.FlightUpdates
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports._

import scala.util.Try

object ArrivalsRemoval {
  def removeArrivals[A](removals: Iterable[UniqueArrivalLike], arrivals: Map[UniqueArrival, A]): Map[UniqueArrival, A] = {
    val keys = removals.collect { case k: UniqueArrival => k }
    val minusRemovals = arrivals -- keys
    val legacyKeys = removals.collect { case lk: LegacyUniqueArrival => lk }
    if (legacyKeys.nonEmpty) {
      legacyKeys.foldLeft(minusRemovals) {
        case (acc, legacyKey) => acc.view.filterKeys(_.legacyUniqueArrival != legacyKey).toMap
      }
    } else minusRemovals
  }
}

class ArrivalsRestorer[A <: WithUnique[UniqueArrival] with Updatable[A]] {
  var arrivals: Map[UniqueArrival, A] = Map()

  def removeHashLegacies(removals: Iterable[Int]): Unit = removals.foreach(keyToRemove => arrivals = arrivals.view.filterKeys(_.legacyUniqueId != keyToRemove).toMap)

  def applyUpdates(updates: Iterable[A]): Unit = updates.foreach { update =>
    val updated = arrivals.get(update.unique).map(_.update(update)).getOrElse(update)
    arrivals = arrivals + ((update.unique, updated))
  }

  def applyUpdates[B](updates: Map[UniqueArrival, B], update: (Option[A], B) => Option[A]): Unit =
    updates.foreach {
      case (key, incoming) =>
        update(arrivals.get(key), incoming).foreach { updated =>
          arrivals = arrivals + ((key, updated))
        }
    }

  def remove(removals: Iterable[UniqueArrivalLike]): Unit =
    arrivals = ArrivalsRemoval.removeArrivals(removals, arrivals)

  def finish(): Unit = arrivals = Map()
}

case class FlightsWithSplitsDiff(flightsToUpdate: Iterable[ApiFlightWithSplits], arrivalsToRemove: Iterable[UniqueArrivalLike]) extends FlightUpdates {
  def latestUpdateMillis: Long = Try(flightsToUpdate.map(_.lastUpdated.getOrElse(0L)).max).getOrElse(0L)

  def isEmpty: Boolean = flightsToUpdate.isEmpty && arrivalsToRemove.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def updateMinutes(sourceOrderPreference: List[FeedSource]): Set[Long] = flightsToUpdate.flatMap(_.apiFlight.pcpRange(sourceOrderPreference)).toSet

  def applyTo(flightsWithSplits: FlightsWithSplits,
              nowMillis: Long,
              sourceOrderPreference: List[FeedSource],
             ): (FlightsWithSplits, Set[Long], Iterable[ApiFlightWithSplits]) = {
    val updatedFlights = flightsToUpdate.map(_.copy(lastUpdated = Option(nowMillis)))
    val updated = flightsWithSplits.flights ++ updatedFlights.map(f => f.unique -> f)

    val minusRemovals: Map[UniqueArrival, ApiFlightWithSplits] = ArrivalsRemoval.removeArrivals(arrivalsToRemove, updated)

    val asMap: Map[UniqueArrival, ApiFlightWithSplits] = flightsWithSplits.flights

    val minutesFromRemovalsInExistingState: Set[Long] = arrivalsToRemove
      .flatMap {
        case r: UniqueArrival =>
          asMap.get(r).map(_.apiFlight.pcpRange(sourceOrderPreference)).getOrElse(List())
        case r: LegacyUniqueArrival =>
          asMap.collect { case (ua, a) if ua.equalsLegacy(r) => a }.flatMap(_.apiFlight.pcpRange(sourceOrderPreference))
      }.toSet

    val minutesFromExistingStateUpdatedFlights = flightsToUpdate
      .flatMap { fws =>
        asMap.get(fws.unique) match {
          case None => Set()
          case Some(f) => f.apiFlight.pcpRange(sourceOrderPreference)
        }
      }.toSet

    val updatedMinutesFromFlights = minutesFromRemovalsInExistingState ++
      updateMinutes(sourceOrderPreference) ++
      minutesFromExistingStateUpdatedFlights

    (FlightsWithSplits(minusRemovals), updatedMinutesFromFlights, updatedFlights)
  }

  lazy val terminals: Set[Terminal] = flightsToUpdate.map(_.apiFlight.Terminal).toSet ++
    arrivalsToRemove.map(_.terminal).toSet

  def ++(other: FlightsWithSplitsDiff): FlightsWithSplitsDiff =
    FlightsWithSplitsDiff(flightsToUpdate ++ other.flightsToUpdate, arrivalsToRemove ++ other.arrivalsToRemove)

  def window(startMillis: Long, endMillis: Long): FlightsWithSplitsDiff =
    FlightsWithSplitsDiff(flightsToUpdate.filter(fws =>
      startMillis <= fws.apiFlight.Scheduled && fws.apiFlight.Scheduled <= endMillis
    ), arrivalsToRemove.filter(ua =>
      startMillis <= ua.scheduled && ua.scheduled <= endMillis
    ))

  def forTerminal(terminal: Terminal): FlightsWithSplitsDiff = FlightsWithSplitsDiff(
    flightsToUpdate.filter(_.apiFlight.Terminal == terminal),
    arrivalsToRemove.filter(_.terminal == terminal)
  )
}

object FlightsWithSplitsDiff {
  val empty: FlightsWithSplitsDiff = FlightsWithSplitsDiff(List(), List())
}
