package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages
import uk.gov.homeoffice.drt.ports.{AclFeedSource, ApiFeedSource, ForecastFeedSource, LiveFeedSource, ScenarioSimulationSource, UnknownFeedSource}
import uk.gov.homeoffice.drt.time.SDateLike
import upickle.default.{ReadWriter, macroRW}


trait WithLastUpdated {
  def lastUpdated: Option[Long]
}

object ApiFlightWithSplits {
  implicit val rw: ReadWriter[ApiFlightWithSplits] = macroRW

  def fromArrival(arrival: Arrival): ApiFlightWithSplits = ApiFlightWithSplits(arrival, Set())
}

case class ApiFlightWithSplits(apiFlight: Arrival, splits: Set[Splits], lastUpdated: Option[Long] = None)
  extends WithUnique[UniqueArrival]
    with Updatable[ApiFlightWithSplits]
    with WithLastUpdated {

  def totalPaxFromApi: Option[TotalPaxSource] = splits.collectFirst {
    case splits if splits.source == ApiSplitsWithHistoricalEGateAndFTPercentages =>
      TotalPaxSource(Math.round(splits.totalPax).toInt, ApiFeedSource, Option(ApiSplitsWithHistoricalEGateAndFTPercentages))
  }

  def totalPaxFromApiExcludingTransfer: Option[TotalPaxSource] =
    splits.collectFirst { case splits if splits.source == ApiSplitsWithHistoricalEGateAndFTPercentages =>
      TotalPaxSource(Math.round(splits.totalExcludingTransferPax).toInt, ApiFeedSource, Option(ApiSplitsWithHistoricalEGateAndFTPercentages))
    }

  def pcpPaxEstimate: TotalPaxSource =
    totalPaxFromApiExcludingTransfer match {
      case Some(totalPaxSource) if hasValidApi => totalPaxSource
      case _ => apiFlight.bestPcpPaxEstimate
    }

  def totalPax: Option[TotalPaxSource] =
    if (hasValidApi) totalPaxFromApi
    else bestSource(apiFlight.ActPax)

  def equals(candidate: ApiFlightWithSplits): Boolean =
    this.copy(lastUpdated = None) == candidate.copy(lastUpdated = None)

  def bestSource(actPax: Option[Int]): Option[TotalPaxSource] = {
    this.apiFlight.FeedSources match {
      case feedSource if feedSource.contains(LiveFeedSource) =>
        Some(TotalPaxSource(actPax.getOrElse(0), LiveFeedSource, None))
      case feedSource if feedSource.contains(ForecastFeedSource) =>
        Some(TotalPaxSource(actPax.getOrElse(0), ForecastFeedSource, None))
      case feedSource if feedSource.contains(AclFeedSource) =>
        Some(TotalPaxSource(actPax.getOrElse(0), AclFeedSource, None))
      case _ =>
        None
    }
  }

  def bestSplits: Option[Splits] = {
    val apiSplitsDc = splits.find(s => s.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages)
    val scenarioSplits = splits.find(s => s.source == SplitSources.ScenarioSimulationSplits)
    val historicalSplits = splits.find(_.source == SplitSources.Historical)
    val terminalSplits = splits.find(_.source == SplitSources.TerminalAverage)

    val apiSplits: List[Option[Splits]] = if (hasValidApi) List(apiSplitsDc) else List(scenarioSplits)

    val splitsForConsideration: List[Option[Splits]] = apiSplits ::: List(historicalSplits, terminalSplits)

    splitsForConsideration.find {
      case Some(_) => true
      case _ => false
    }.flatten
  }

  val hasApi: Boolean = splits.exists(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages)

  def hasValidApi: Boolean = {
    val maybeApiSplits = splits.find(_.source == SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages)
    val hasLiveSource = apiFlight.FeedSources.contains(LiveFeedSource)
    val hasSimulationSource = apiFlight.FeedSources.contains(ScenarioSimulationSource)
    (maybeApiSplits, hasLiveSource, hasSimulationSource) match {
      case (Some(_), _, true) => true
      case (Some(_), false, _) => true
      case (Some(api), true, _) if isWithinThreshold(api) => true
      case _ => false
    }
  }

  def isWithinThreshold(apiSplits: Splits): Boolean = apiFlight.ActPax.forall { actPax =>
    val apiPaxNo = apiSplits.totalExcludingTransferPax
    val threshold: Double = 0.05
    val portDirectPax: Double = actPax - apiFlight.TranPax.getOrElse(0)
    apiPaxNo != 0 && Math.abs(apiPaxNo - portDirectPax) / apiPaxNo < threshold
  }

  def hasPcpPaxIn(start: SDateLike, end: SDateLike): Boolean = apiFlight.hasPcpDuring(start, end)

  override val unique: UniqueArrival = apiFlight.unique

  override def update(incoming: ApiFlightWithSplits): ApiFlightWithSplits =
    incoming.copy(apiFlight = apiFlight.update(incoming.apiFlight))
}
