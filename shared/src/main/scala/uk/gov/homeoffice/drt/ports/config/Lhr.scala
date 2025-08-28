package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.LHR
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.time.LocalDate

import scala.collection.immutable.SortedMap

object Lhr extends AirportConfigLike {
  val lhrDefaultQueueRatios: Map[PaxType, Seq[(Queue, Double)]] = Map(
    GBRNational -> List(Queues.EGate -> 0.80, Queues.EeaDesk -> 0.20),
    GBRNationalBelowEgateAge -> List(Queues.EeaDesk -> 1.0),
    EeaMachineReadable -> List(Queues.EGate -> 0.80, Queues.EeaDesk -> 0.20),
    EeaBelowEGateAge -> List(Queues.EeaDesk -> 1.0),
    EeaNonMachineReadable -> List(Queues.EeaDesk -> 1.0),
    Transit -> List(Queues.Transfer -> 1.0),
    NonVisaNational -> List(Queues.NonEeaDesk -> 1.0),
    VisaNational -> List(Queues.NonEeaDesk -> 1.0),
    B5JPlusNational -> List(Queues.EGate -> 0.70, Queues.EeaDesk -> 0.30),
    B5JPlusNationalBelowEGateAge -> List(Queues.EeaDesk -> 1)
  )

  private object ProcTimesT2 {
    val gbr = 38.0
    val eea = 50.0
    val b5jssk = 61.0
    val nvn = 92.0
    val vn = 97.0
    val egates = 44d
  }

  private object ProcTimesT3 {
    val gbr = 37.0
    val eea = 46.0
    val b5jssk = 56.0
    val nvn = 90.0
    val vn = 100.0
    val egates = 44d
  }

  private object ProcTimesT4 {
    val gbr = 38.0
    val eea = 49.0
    val b5jssk = 62.0
    val nvn = 84.0
    val vn = 94.0
    val egates = 44d
  }

  private object ProcTimesT5 {
    val gbr = 36.0
    val eea = 45.0
    val b5jssk = 56.0
    val nvn = 88.0
    val vn = 107.0
    val egates = 47d
  }

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("LHR"),
    portName = "London Heathrow",
    queuesByTerminal = SortedMap(LocalDate(2014, 1, 1) -> SortedMap(
      T2 -> Seq(EeaDesk, EGate, NonEeaDesk, FastTrack, Transfer),
      T3 -> Seq(EeaDesk, EGate, NonEeaDesk, FastTrack, Transfer),
      T4 -> Seq(EeaDesk, EGate, NonEeaDesk, FastTrack, Transfer),
      T5 -> Seq(EeaDesk, EGate, NonEeaDesk, FastTrack, Transfer)
    )),
    slaByQueue = Map(EeaDesk -> 25, EGate -> 15, NonEeaDesk -> 45, FastTrack -> 15),
    crunchOffsetMinutes = 120,
    defaultWalkTimeMillis = Map(T2 -> 900000L, T3 -> 660000L, T4 -> 900000L, T5 -> 660000L),
    terminalPaxSplits = List(T2, T3, T4, T5).map(t => (t, SplitRatios(
      SplitSources.TerminalAverage,
      SplitRatio(eeaMachineReadableToDesk, 0.64 * 0.2),
      SplitRatio(eeaMachineReadableToEGate, 0.64 * 0.8),
      SplitRatio(eeaNonMachineReadableToDesk, 0),
      SplitRatio(visaNationalToDesk, 0.08),
      SplitRatio(visaNationalToFastTrack, 0),
      SplitRatio(nonVisaNationalToDesk, 0.28),
      SplitRatio(nonVisaNationalToFastTrack, 0)
    ))).toMap,
    terminalProcessingTimes = Map(
      T2 -> Map(
        b5jsskToDesk -> ProcTimesT2.b5jssk / 60,
        b5jsskChildToDesk -> ProcTimesT2.b5jssk / 60,
        eeaMachineReadableToDesk -> ProcTimesT2.eea / 60,
        eeaNonMachineReadableToDesk -> ProcTimesT2.eea / 60,
        eeaChildToDesk -> ProcTimesT2.eea / 60,
        gbrNationalToDesk -> ProcTimesT2.gbr / 60,
        gbrNationalChildToDesk -> ProcTimesT2.gbr / 60,
        b5jsskToEGate -> ProcTimesT2.egates / 60,
        eeaMachineReadableToEGate -> ProcTimesT2.egates / 60,
        gbrNationalToEgate -> ProcTimesT2.egates / 60,
        visaNationalToDesk -> ProcTimesT2.vn / 60,
        nonVisaNationalToDesk -> ProcTimesT2.nvn / 60,
        visaNationalToFastTrack -> ProcTimesT2.vn / 60,
        nonVisaNationalToFastTrack -> ProcTimesT2.nvn / 60,
        transitToTransfer -> 50d / 60,
      ),
      T3 -> Map(
        b5jsskToDesk -> ProcTimesT3.b5jssk / 60,
        b5jsskChildToDesk -> ProcTimesT3.b5jssk / 60,
        eeaMachineReadableToDesk -> ProcTimesT3.eea / 60,
        eeaNonMachineReadableToDesk -> ProcTimesT3.eea / 60,
        eeaChildToDesk -> ProcTimesT3.eea / 60,
        gbrNationalToDesk -> ProcTimesT3.gbr / 60,
        gbrNationalChildToDesk -> ProcTimesT3.gbr / 60,
        b5jsskToEGate -> ProcTimesT3.egates / 60,
        eeaMachineReadableToEGate -> ProcTimesT3.egates / 60,
        gbrNationalToEgate -> ProcTimesT3.egates / 60,
        visaNationalToDesk -> ProcTimesT3.vn / 60,
        nonVisaNationalToDesk -> ProcTimesT3.nvn / 60,
        visaNationalToFastTrack -> ProcTimesT3.vn / 60,
        nonVisaNationalToFastTrack -> ProcTimesT3.nvn / 60,
        transitToTransfer -> 50d / 60,
      ),
      T4 -> Map(
        b5jsskToDesk -> ProcTimesT4.b5jssk / 60,
        b5jsskChildToDesk -> ProcTimesT4.b5jssk / 60,
        eeaMachineReadableToDesk -> ProcTimesT4.eea / 60,
        eeaNonMachineReadableToDesk -> ProcTimesT4.eea / 60,
        eeaChildToDesk -> ProcTimesT4.eea / 60,
        gbrNationalToDesk -> ProcTimesT4.gbr / 60,
        gbrNationalChildToDesk -> ProcTimesT4.gbr / 60,
        b5jsskToEGate -> ProcTimesT4.egates / 60,
        eeaMachineReadableToEGate -> ProcTimesT4.egates / 60,
        gbrNationalToEgate -> ProcTimesT4.egates / 60,
        visaNationalToDesk -> ProcTimesT4.vn / 60,
        nonVisaNationalToDesk -> ProcTimesT4.nvn / 60,
        visaNationalToFastTrack -> ProcTimesT4.vn / 60,
        nonVisaNationalToFastTrack -> ProcTimesT4.nvn / 60,
        transitToTransfer -> 50d / 60,
      ),
      T5 -> Map(
        b5jsskToDesk -> ProcTimesT5.b5jssk / 60,
        b5jsskChildToDesk -> ProcTimesT5.b5jssk / 60,
        eeaMachineReadableToDesk -> ProcTimesT5.eea / 60,
        eeaNonMachineReadableToDesk -> ProcTimesT5.eea / 60,
        eeaChildToDesk -> ProcTimesT5.eea / 60,
        gbrNationalToDesk -> ProcTimesT5.gbr / 60,
        gbrNationalChildToDesk -> ProcTimesT5.gbr / 60,
        b5jsskToEGate -> ProcTimesT5.egates / 60,
        eeaMachineReadableToEGate -> ProcTimesT5.egates / 60,
        gbrNationalToEgate -> ProcTimesT5.egates / 60,
        visaNationalToDesk -> ProcTimesT5.vn / 60,
        nonVisaNationalToDesk -> ProcTimesT5.nvn / 60,
        visaNationalToFastTrack -> ProcTimesT5.vn / 60,
        nonVisaNationalToFastTrack -> ProcTimesT5.nvn / 60,
        transitToTransfer -> 50d / 60,
      )
    ),
    minMaxDesksByTerminalQueue24Hrs = Map(
      T2 -> Map(
        Queues.EGate -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)),
        Queues.EeaDesk -> (List(0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2), List(9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9)),
        Queues.FastTrack -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6)),
        Queues.NonEeaDesk -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20))
      ),
      T3 -> Map(
        Queues.EGate -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)),
        Queues.EeaDesk -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16)),
        Queues.FastTrack -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7)),
        Queues.NonEeaDesk -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23))
      ),
      T4 -> Map(
        Queues.EGate -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
        Queues.EeaDesk -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)),
        Queues.FastTrack -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)),
        Queues.NonEeaDesk -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27))
      ),
      T5 -> Map(
        Queues.EGate -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)),
        Queues.EeaDesk -> (List(0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2), List(6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6)),
        Queues.FastTrack -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(0, 0, 0, 0, 0, 2, 2, 2, 1, 1, 1, 1, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 0)),
        Queues.NonEeaDesk -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20))
      )
    ),
    eGateBankSizes = Map(
      T2 -> Iterable(10, 5),
      T3 -> Iterable(10, 5),
      T4 -> Iterable(10),
      T5 -> Iterable(10, 9, 5),
    ),
    hasActualDeskStats = true,
    forecastExportQueueOrder = Queues.forecastExportQueueOrderWithFastTrack,
    desksExportQueueOrder = Queues.deskExportQueueOrderWithFastTrack,
    role = LHR,
    terminalPaxTypeQueueAllocation = {
      val egateSplitT2 = 0.8102
      val egateSplitT3 = 0.8075
      val egateSplitT4 = 0.7687
      val egateSplitT5 = 0.8466
      Map(
        T2 -> (lhrDefaultQueueRatios + (EeaMachineReadable -> List(
          EGate -> egateSplitT2,
          EeaDesk -> (1.0 - egateSplitT2)
        ))),
        T3 -> (lhrDefaultQueueRatios + (EeaMachineReadable -> List(
          EGate -> egateSplitT3,
          EeaDesk -> (1.0 - egateSplitT3)
        ))),
        T4 -> (lhrDefaultQueueRatios + (EeaMachineReadable -> List(
          EGate -> egateSplitT4,
          EeaDesk -> (1.0 - egateSplitT4)
        ))),
        T5 -> (lhrDefaultQueueRatios + (EeaMachineReadable -> List(
          EGate -> egateSplitT5,
          EeaDesk -> (1.0 - egateSplitT5)
        )))
      )
    },
    hasTransfer = true,
    maybeCiriumEstThresholdHours = Option(6),
    feedSources = Seq(ApiFeedSource, LiveBaseFeedSource, LiveFeedSource, ForecastFeedSource, AclFeedSource),
    flexedQueues = Set(EeaDesk, NonEeaDesk),
    desksByTerminal = Map[Terminal, Int](
      T2 -> 29,
      T3 -> 28,
      T4 -> 39,
      T5 -> 27,
    )
  )
}
