package uk.gov.homeoffice.drt.ports

import ujson.Value.Value
import upickle.default._

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait FeedSource {
  def name: String

  def displayName: String = name

  val maybeLastUpdateThreshold: Option[FiniteDuration]

  val description: Boolean => String

  override val toString: String = getClass.getSimpleName.split("\\$").last
}

case object HistoricApiFeedSource extends FeedSource {
  override val name: String = "Historic API"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = None

  override val description: Boolean => String = isLiveFeedAvailable => if (isLiveFeedAvailable)
    "Historic passenger nationality and age data when available."
  else
    "Historic passenger numbers and nationality data when available."
}

case object ApiFeedSource extends FeedSource {
  override val name: String = "API"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = None

  override val description: Boolean => String = isLiveFeedAvailable => if (isLiveFeedAvailable)
    "Actual passenger nationality and age data when available."
  else
    "Actual passenger numbers and nationality data when available."
}

case object AclFeedSource extends FeedSource {
  override val name: String = "ACL"

  override val displayName: String = "Forecast schedule"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = Option(36.hours)

  override val description: Boolean => String = _ => "Flight schedule for up to 6 months."
}

case object ForecastFeedSource extends FeedSource {
  override val name: String = "Port forecast"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = None

  override val description: Boolean => String = _ => "Updated forecast of passenger numbers."
}

case object LiveFeedSource extends FeedSource {
  override val name: String = "Port live"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = Option(12.hours)

  override val description: Boolean => String = isCiriumAsLiveFeedSource => if (isCiriumAsLiveFeedSource)
    "Estimated and actual arrival time updates where not available from the port operator."
  else
    "Up-to-date passenger numbers, estimated and actual arrival times, gates and stands."

}

case object ScenarioSimulationSource extends FeedSource {
  override val name: String = "Scenario Simulation"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = Option(12.hours)

  override val description: Boolean => String = _ => "An altered arrival to explore a simulated scenario."
}

case object LiveBaseFeedSource extends FeedSource {
  override val name: String = "Cirium live"

  override val displayName: String = "Live arrival"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = Option(12.hours)

  override val description: Boolean => String = isLiveFeedAvailable => if (isLiveFeedAvailable)
    "Estimated and actual arrival time updates where not available from live feed."
  else
    "Estimated and actual arrival time updates."
}

case object UnknownFeedSource extends FeedSource {
  override val name: String = "Unknown"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = None

  override val description: Boolean => String = _ => ""
}

object FeedSource {
  def feedSources: Set[FeedSource] = Set(ApiFeedSource, AclFeedSource, ForecastFeedSource, HistoricApiFeedSource, LiveFeedSource, LiveBaseFeedSource, ScenarioSimulationSource)

  def apply(feedSource: String): Option[FeedSource] = feedSources.find(fs => fs.toString == feedSource)

  def findByName(feedSource: String): Option[FeedSource] = feedSources.find(fs => fs.name == feedSource)

  implicit val feedSourceReadWriter: ReadWriter[FeedSource] =
    readwriter[Value].bimap[FeedSource](
      feedSource => feedSource.toString,
      (s: Value) => apply(s.str).getOrElse(UnknownFeedSource)
    )
}
