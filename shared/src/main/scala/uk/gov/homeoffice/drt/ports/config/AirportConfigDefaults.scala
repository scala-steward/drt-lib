package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk, NonEeaDesk, Queue}
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports.{PaxTypes, _}

object AirportConfigDefaults {
  val defaultSlas: Map[Queue, Int] = Map(
    EeaDesk -> 20,
    EGate -> 25,
    NonEeaDesk -> 45
  )

  val defaultPaxSplits: SplitRatios = SplitRatios(
    SplitSources.TerminalAverage,
    SplitRatio(eeaMachineReadableToDesk, 0.175),
    SplitRatio(eeaMachineReadableToEGate, 0.55),
    SplitRatio(eeaNonMachineReadableToDesk, 0.175),
    SplitRatio(visaNationalToDesk, 0.05),
    SplitRatio(nonVisaNationalToDesk, 0.05)
  )

  val defaultPaxSplitsWithoutEgates: SplitRatios = SplitRatios(
    SplitSources.TerminalAverage,
    SplitRatio(eeaMachineReadableToDesk, 0.725),
    SplitRatio(eeaNonMachineReadableToDesk, 0.175),
    SplitRatio(visaNationalToDesk, 0.05),
    SplitRatio(nonVisaNationalToDesk, 0.05)
  )

  val defaultQueueRatios: Map[PaxType, Seq[(Queue, Double)]] = Map(
    GBRNational -> List(EGate -> 0.8, EeaDesk -> 0.2),
    GBRNationalBelowEgateAge -> List(EeaDesk -> 1.0),
    EeaMachineReadable -> List(EGate -> 0.8, EeaDesk -> 0.2),
    EeaBelowEGateAge -> List(EeaDesk -> 1.0),
    EeaNonMachineReadable -> List(EeaDesk -> 1.0),
    NonVisaNational -> List(NonEeaDesk -> 1.0),
    VisaNational -> List(NonEeaDesk -> 1.0),
    B5JPlusNational -> List(EGate -> 0.7, EeaDesk -> 0.3),
    B5JPlusNationalBelowEGateAge -> List(EeaDesk -> 1),
    PaxTypes.Transit -> List(),
  )

  val defaultQueueRatiosWithoutEgates: Map[PaxType, Seq[(Queue, Double)]] = defaultQueueRatios ++ Map(
    GBRNational -> List(EeaDesk -> 1.0),
    EeaMachineReadable -> List(EeaDesk -> 1.0),
    B5JPlusNational -> List(EeaDesk -> 1.0),
  )

  private object ProcTimes {
    val gbr = 22.7
    val eea = 26.2
    val b5jssk = 45.5
    val nvn = 91.4
    val vn = 99.5
    val egates = 36d
  }

  val defaultProcessingTimes: Map[PaxTypeAndQueue, Double] = Map(
    b5jsskToDesk -> ProcTimes.b5jssk / 60,
    b5jsskChildToDesk -> ProcTimes.b5jssk / 60,
    eeaChildToDesk -> ProcTimes.eea / 60,
    eeaMachineReadableToDesk -> ProcTimes.eea / 60,
    eeaNonMachineReadableToDesk -> ProcTimes.eea / 60,
    gbrNationalToDesk -> ProcTimes.gbr / 60,
    gbrNationalChildToDesk -> ProcTimes.gbr / 60,
    b5jsskToEGate -> ProcTimes.egates / 60,
    eeaMachineReadableToEGate -> ProcTimes.egates / 60,
    gbrNationalToEgate -> ProcTimes.egates / 60,
    visaNationalToDesk -> ProcTimes.vn / 60,
    nonVisaNationalToDesk -> ProcTimes.nvn / 60
  )

  val fallbackProcessingTime: Double = defaultProcessingTimes.values.sum / defaultProcessingTimes.size
}
