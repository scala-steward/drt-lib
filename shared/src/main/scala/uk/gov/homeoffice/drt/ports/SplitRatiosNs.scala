package uk.gov.homeoffice.drt.ports

import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.{AdvPaxInfo, ApiSplitsWithHistoricalEGateAndFTPercentages, ApiSplitsWithHistoricalEGateAndFTPercentages_Old, Historical, InvalidSource, PredictedSplitsWithHistoricalEGateAndFTPercentages, ScenarioSimulationSplits, TerminalAverage}
import upickle.default.{ReadWriter, macroRW}

object SplitRatiosNs {

  case class SplitRatios(splits: Iterable[SplitRatio] = Nil, origin: SplitSource)

  sealed trait SplitSource extends ClassNameForToString {
    val id: Int
  }

  object SplitSource {
    implicit val rw: ReadWriter[SplitSource] = ReadWriter.merge(
      macroRW[AdvPaxInfo.type],
      macroRW[ApiSplitsWithHistoricalEGateAndFTPercentages_Old.type],
      macroRW[ApiSplitsWithHistoricalEGateAndFTPercentages.type],
      macroRW[PredictedSplitsWithHistoricalEGateAndFTPercentages.type],
      macroRW[ScenarioSimulationSplits.type],
      macroRW[Historical.type],
      macroRW[TerminalAverage.type],
      macroRW[InvalidSource.type],
    )

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
    def apply(id: Int): SplitSource = id match {
      case 1 => AdvPaxInfo
      case 2 => ApiSplitsWithHistoricalEGateAndFTPercentages_Old
      case 3 => ApiSplitsWithHistoricalEGateAndFTPercentages
      case 4 => PredictedSplitsWithHistoricalEGateAndFTPercentages
      case 5 => ScenarioSimulationSplits
      case 6 => Historical
      case 7 => TerminalAverage
      case _ => InvalidSource
    }
  }

  object SplitSources {

    object AdvPaxInfo extends SplitSource {
      override val id: Int = 1
    }

    object ApiSplitsWithHistoricalEGateAndFTPercentages_Old extends SplitSource {
      override val id: Int = 2
    }

    object ApiSplitsWithHistoricalEGateAndFTPercentages extends SplitSource {
      override val id: Int = 3
    }

    object PredictedSplitsWithHistoricalEGateAndFTPercentages extends SplitSource {
      override val id: Int = 4
    }

    object ScenarioSimulationSplits extends SplitSource {
      override val id: Int = 5
    }

    object Historical extends SplitSource {
      override val id: Int = 6
    }

    object TerminalAverage extends SplitSource {
      override val id: Int = 7
    }

    object InvalidSource extends SplitSource {
      override val id: Int = -1
    }

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
