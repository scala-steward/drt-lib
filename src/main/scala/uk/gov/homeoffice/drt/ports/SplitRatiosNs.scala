package uk.gov.homeoffice.drt.ports

import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.{AdvPaxInfo, ApiSplitsWithHistoricalEGateAndFTPercentages, ApiSplitsWithHistoricalEGateAndFTPercentages_Old, Historical, InvalidSource, PredictedSplitsWithHistoricalEGateAndFTPercentages, ScenarioSimulationSplits, TerminalAverage}
import upickle.default.{ReadWriter, macroRW}

object SplitRatiosNs {

  case class SplitRatios(splits: Iterable[SplitRatio] = Nil, origin: SplitSource)

  sealed trait SplitSource extends ClassNameForToString

  object SplitSource {
    implicit val rw: ReadWriter[SplitSource] = macroRW

    def apply(splitSource: String): SplitSource = splitSource match {
      case "advPaxInfo" => AdvPaxInfo
      case "ApiSplitsWithHistoricalEGatePercentage" => ApiSplitsWithHistoricalEGateAndFTPercentages_Old
      case "ApiSplitsWithHistoricalEGateAndFTPercentages" => ApiSplitsWithHistoricalEGateAndFTPercentages
      case "PredictedSplitsWithHistoricalEGateAndFTPercentages" => PredictedSplitsWithHistoricalEGateAndFTPercentages
      case "ScenarioSimulationSplits" => ScenarioSimulationSplits
      case "Historical" => Historical
      case "TerminalAverage" => TerminalAverage
      case _ => InvalidSource
    }
  }

  object SplitSources {

    object AdvPaxInfo extends SplitSource

    object ApiSplitsWithHistoricalEGateAndFTPercentages_Old extends SplitSource

    object ApiSplitsWithHistoricalEGateAndFTPercentages extends SplitSource

    object PredictedSplitsWithHistoricalEGateAndFTPercentages extends SplitSource

    object ScenarioSimulationSplits extends SplitSource

    object Historical extends SplitSource

    object TerminalAverage extends SplitSource

    object InvalidSource extends SplitSource

  }

  object SplitRatios {
    def apply(origin: SplitSource, ratios: SplitRatio*): SplitRatios = SplitRatios(ratios.toList, origin)

    def apply(origin: SplitSource, ratios: List[SplitRatio]): SplitRatios = SplitRatios(ratios, origin)

    implicit val rw: ReadWriter[SplitRatios] = macroRW
  }

  case class SplitRatio(paxType: PaxTypeAndQueue, ratio: Double)

  object SplitRatio {
    implicit val rw: ReadWriter[SplitRatio] = macroRW
  }

}
