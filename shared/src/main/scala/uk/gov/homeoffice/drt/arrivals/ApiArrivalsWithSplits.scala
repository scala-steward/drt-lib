package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.arrivals.ApiFlightWithSplits.liveApiTolerance
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages
import uk.gov.homeoffice.drt.ports._
import upickle.default.{ReadWriter, macroRW}


trait WithLastUpdated {
  def lastUpdated: Option[Long]
}

object ApiFlightWithSplits {
  implicit val rw: ReadWriter[ApiFlightWithSplits] = macroRW

  def fromArrival(arrival: Arrival): ApiFlightWithSplits = ApiFlightWithSplits(arrival, Set())

  val liveApiTolerance: Double = 0.05
}

case class ApiFlightWithSplits(apiFlight: Arrival, splits: Set[Splits], lastUpdated: Option[Long] = None)
  extends WithUnique[UniqueArrival]
    with Updatable[ApiFlightWithSplits]
    with WithLastUpdated {

  def paxFromApi: Option[PaxSource] = splits.collectFirst {
    case splits if splits.source == ApiSplitsWithHistoricalEGateAndFTPercentages =>
      PaxSource(ApiFeedSource, Passengers(Option(splits.totalPax), Option(splits.transPax)))
  }

  def equals(candidate: ApiFlightWithSplits): Boolean =
    this.copy(lastUpdated = None) == candidate.copy(lastUpdated = None)


  def bestSplits: Option[Splits] = {
    val apiSplitsDc = splits.find(s => s.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages && (s.totalPax == 0 || s.totalPax != s.transPax))
    val scenarioSplits = splits.find(s => s.source == SplitSources.ScenarioSimulationSplits && (s.totalPax == 0 || s.totalPax != s.transPax))
    val historicalSplits = splits.find(s => s.source == SplitSources.Historical && (s.totalPax == 0 || s.totalPax != s.transPax))
    val terminalSplits = splits.find(_.source == SplitSources.TerminalAverage)

    val apiSplits = if (hasValidApi) List(apiSplitsDc) else List(scenarioSplits)

    val splitsForConsideration: List[Option[Splits]] = apiSplits ::: List(historicalSplits, terminalSplits)

    splitsForConsideration.find {
      case Some(_) => true
      case _ => false
    }.flatten
  }

  val hasApi: Boolean = splits.exists(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages)

  def hasValidApi: Boolean = {
    val maybeApiSplits = splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages)
    val totalPaxSourceIntroductionMillis = 1655247600000L // 2022-06-15 midnight BST

    val paxSourceAvailable = apiFlight.Scheduled >= totalPaxSourceIntroductionMillis
    val hasLiveSource = if (paxSourceAvailable)
      apiFlight.PassengerSources.get(LiveFeedSource).exists(_.actual.nonEmpty)
    else
      apiFlight.FeedSources.contains(LiveFeedSource)

    val hasSimulationSource = apiFlight.FeedSources.contains(ScenarioSimulationSource)
    (maybeApiSplits, hasLiveSource, hasSimulationSource) match {
      case (Some(_), _, true) => true
      case (Some(_), false, _) => true
      case (Some(api), true, _) if api.isWithinThreshold(apiFlight.PassengerSources.get(LiveFeedSource), liveApiTolerance) => true
      case _ => false
    }
  }

  override val unique: UniqueArrival = apiFlight.unique

  override def update(incoming: ApiFlightWithSplits): ApiFlightWithSplits =
    incoming.copy(apiFlight = apiFlight.update(incoming.apiFlight))
}
