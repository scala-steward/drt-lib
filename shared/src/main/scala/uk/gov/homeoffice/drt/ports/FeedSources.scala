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

  val id: String
}

case object MlFeedSource extends FeedSource {
  override val name: String = "Prediction"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = None

  override val description: Boolean => String = _ => "Predicted passenger numbers based on an ML model"

  override val id: String = "ml"
}

case object HistoricApiFeedSource extends FeedSource {
  override val name: String = "Historic API"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = None

  override val description: Boolean => String = isLiveFeedAvailable => if (isLiveFeedAvailable)
    "Historic passenger nationality and age data when available."
  else
    "Historic passenger numbers and nationality data when available."

  override val id: String = "historic-api"
}

case object ApiFeedSource extends FeedSource {
  override val name: String = "API"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = None

  override val description: Boolean => String = isLiveFeedAvailable => if (isLiveFeedAvailable)
    "Actual passenger nationality and age data when available."
  else
    "Actual passenger numbers and nationality data when available."

  override val id: String = "api"
}

case object AclFeedSource extends FeedSource {
  override val name: String = "ACL"

  override val displayName: String = "Forecast schedule"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = Option(36.hours)

  override val description: Boolean => String = _ => "Flight schedule for up to 6 months."

  override val id: String = "acl"
}

case object ForecastFeedSource extends FeedSource {
  override val name: String = "Port forecast"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = None

  override val description: Boolean => String = _ => "Updated forecast of passenger numbers."

  override val id: String = "forecast"
}

case object LiveFeedSource extends FeedSource {
  override val name: String = "Port live"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = Option(12.hours)

  override val description: Boolean => String = isCiriumAsLiveFeedSource => if (isCiriumAsLiveFeedSource)
    "Estimated and actual arrival time updates where not available from the port operator."
  else
    "Up-to-date passenger numbers, estimated and actual arrival times, gates and stands."

  override val id: String = "live"
}

case object ScenarioSimulationSource extends FeedSource {
  override val name: String = "Scenario Simulation"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = Option(12.hours)

  override val description: Boolean => String = _ => "An altered arrival to explore a simulated scenario."

  override val id: String = "scenario-simulation"
}

case object LiveBaseFeedSource extends FeedSource {
  override val name: String = "Cirium live"

  override val displayName: String = "Live arrival"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = Option(12.hours)

  override val description: Boolean => String = isLiveFeedAvailable => if (isLiveFeedAvailable)
    "Estimated and actual arrival time updates where not available from live feed."
  else
    "Estimated and actual arrival time updates."

  override val id: String = "live-base"
}

case object UnknownFeedSource extends FeedSource {
  override val name: String = "Unknown"

  override val maybeLastUpdateThreshold: Option[FiniteDuration] = None

  override val description: Boolean => String = _ => ""

  override val id: String = "unknown"
}

object FeedSource {
  def feedSources: Set[FeedSource] = Set(ApiFeedSource, AclFeedSource, ForecastFeedSource, HistoricApiFeedSource, LiveFeedSource, LiveBaseFeedSource, ScenarioSimulationSource, MlFeedSource)

  def apply(feedSource: String): Option[FeedSource] = feedSources.find(fs => fs.toString == feedSource || fs.name == feedSource)

  def byId(id: String): FeedSource = id match {
    case "api" => ApiFeedSource
    case "acl" => AclFeedSource
    case "forecast" => ForecastFeedSource
    case "historic-api" => HistoricApiFeedSource
    case "live" => LiveFeedSource
    case "live-base" => LiveBaseFeedSource
    case "scenario-simulation" => ScenarioSimulationSource
    case "ml" => MlFeedSource
    case _ => UnknownFeedSource
  }

  implicit val feedSourceReadWriter: ReadWriter[FeedSource] =
    readwriter[Value].bimap[FeedSource](
      feedSource => feedSource.toString,
      (s: Value) => apply(s.str).getOrElse(UnknownFeedSource)
    )
}
