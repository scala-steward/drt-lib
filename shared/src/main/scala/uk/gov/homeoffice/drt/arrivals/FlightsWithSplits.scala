package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.{AclFeedSource, ApiFeedSource, FeedSource, ForecastFeedSource, HistoricApiFeedSource, LiveFeedSource, MlFeedSource, ScenarioSimulationSource}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.SDateLike

import scala.util.Try

case class FlightsWithSplits(flights: Map[UniqueArrival, ApiFlightWithSplits]) {
  def latestUpdateMillis: Long = Try(flights.map(_._2.lastUpdated.getOrElse(0L)).max).getOrElse(0L)

  val isEmpty: Boolean = flights.isEmpty
  val nonEmpty: Boolean = !isEmpty

  def scheduledSince(sinceMillis: Long): FlightsWithSplits = FlightsWithSplits(flights.filter {
    case (UniqueArrival(_, _, scheduledMillis, _), _) => scheduledMillis >= sinceMillis
  })

  def scheduledOrPcpWindow(start: SDateLike, end: SDateLike, sourceOrderPreference: List[FeedSource]): FlightsWithSplits = {
    val inWindow = flights.filter {
      case (_, fws) =>
        val pcpMatches = fws.apiFlight.hasPcpDuring(start, end, sourceOrderPreference)
        val scheduledMatches = start <= fws.apiFlight.Scheduled && end >= fws.apiFlight.Scheduled
        scheduledMatches || pcpMatches
    }
    FlightsWithSplits(inWindow)
  }

  def forTerminal(terminal: Terminal): FlightsWithSplits = {
    val inTerminal = flights.filter {
      case (_, fws) => fws.apiFlight.Terminal == terminal
    }
    FlightsWithSplits(inTerminal)
  }

  def updatedSince(sinceMillis: Long): FlightsWithSplits =
    FlightsWithSplits(flights.filter {
      case (_, fws) => fws.lastUpdated.getOrElse(0L) > sinceMillis
    })

  def --(toRemove: Iterable[UniqueArrival]): FlightsWithSplits = FlightsWithSplits(flights -- toRemove)

  def ++(toUpdate: Iterable[(UniqueArrival, ApiFlightWithSplits)]): FlightsWithSplits = FlightsWithSplits(flights ++ toUpdate)

  def +(toAdd: ApiFlightWithSplits): FlightsWithSplits = FlightsWithSplits(flights.updated(toAdd.unique, toAdd))

  def ++(other: FlightsWithSplits): FlightsWithSplits = FlightsWithSplits(flights ++ other.flights)
}

object FlightsWithSplits {
  val empty: FlightsWithSplits = FlightsWithSplits(Map[UniqueArrival, ApiFlightWithSplits]())

  def apply(flights: Iterable[ApiFlightWithSplits]): FlightsWithSplits = FlightsWithSplits(flights.map(fws => (fws.unique, fws)).toMap)
}
