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

case class FlightsWithSplitsDiff(flightsToUpdate: Iterable[ApiFlightWithSplits]) extends FlightUpdates {
  def latestUpdateMillis: Long = Try(flightsToUpdate.map(_.lastUpdated.getOrElse(0L)).max).getOrElse(0L)

  def isEmpty: Boolean = flightsToUpdate.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def updateMinutes(sourceOrderPreference: List[FeedSource]): Set[Long] = flightsToUpdate.flatMap(_.apiFlight.pcpRange(sourceOrderPreference)).toSet

  def applyTo(flightsWithSplits: FlightsWithSplits,
              nowMillis: Long,
              sourceOrderPreference: List[FeedSource],
             ): (FlightsWithSplits, Set[Long], Iterable[ApiFlightWithSplits], Iterable[UniqueArrival]) = {
    val updatedFlights = flightsToUpdate.map(_.copy(lastUpdated = Option(nowMillis)))
    val updated = flightsWithSplits.flights ++ updatedFlights.map(f => f.unique -> f)

    val asMap: Map[UniqueArrival, ApiFlightWithSplits] = flightsWithSplits.flights

    val minutesFromExistingStateUpdatedFlights = flightsToUpdate
      .flatMap { fws =>
        asMap.get(fws.unique) match {
          case None => Set()
          case Some(f) => f.apiFlight.pcpRange(sourceOrderPreference)
        }
      }.toSet

    val updatedMinutesFromFlights =
      updateMinutes(sourceOrderPreference) ++
      minutesFromExistingStateUpdatedFlights

    (FlightsWithSplits(updated), updatedMinutesFromFlights, updatedFlights, Seq.empty)
  }

  lazy val terminals: Set[Terminal] = flightsToUpdate.map(_.apiFlight.Terminal).toSet

  def ++(other: FlightsWithSplitsDiff): FlightsWithSplitsDiff =
    FlightsWithSplitsDiff(flightsToUpdate ++ other.flightsToUpdate)

  def window(startMillis: Long, endMillis: Long): FlightsWithSplitsDiff =
    FlightsWithSplitsDiff(flightsToUpdate.filter(fws =>
      startMillis <= fws.apiFlight.Scheduled && fws.apiFlight.Scheduled <= endMillis
    ))

  def forTerminal(terminal: Terminal): FlightsWithSplitsDiff =
    FlightsWithSplitsDiff(flightsToUpdate.filter(_.apiFlight.Terminal == terminal))
}

object FlightsWithSplitsDiff {
  val empty: FlightsWithSplitsDiff = FlightsWithSplitsDiff(Seq.empty)
}
