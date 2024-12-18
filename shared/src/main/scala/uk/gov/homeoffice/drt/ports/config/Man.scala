package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.MAN
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._

import scala.collection.immutable.SortedMap

object Man extends AirportConfigLike {

  import AirportConfigDefaults._

  private object ProcTimesT1 {
    val gbr = 30.8
    val eea = 40.9
    val b5jssk = 52.7
    val nvn = 89.1
    val vn = 99.9
  }

  private object ProcTimesT2 {
    val gbr = 30.3
    val eea = 43.4
    val b5jssk = 59.8
    val nvn = 101.3
    val vn = 98.2
  }

  private object ProcTimesT3 {
    val gbr = 31.8
    val eea = 39.8
    val b5jssk = 57
    val nvn = 86.4
    val vn = 91.6
  }

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("MAN"),
    portName = "Manchester",
    queuesByTerminal = SortedMap(
      T1 -> Seq(EeaDesk, EGate, NonEeaDesk),
      T2 -> Seq(EeaDesk, EGate, NonEeaDesk),
      T3 -> Seq(EeaDesk, EGate, NonEeaDesk)
    ),
    slaByQueue = Map(EeaDesk -> 25, EGate -> 10, NonEeaDesk -> 45),
    defaultWalkTimeMillis = Map(T1 -> 180000L, T2 -> 600000L, T3 -> 180000L),
    terminalPaxSplits = List(T1, T2, T3).map(t => (t, SplitRatios(
      SplitSources.TerminalAverage,
      SplitRatio(eeaMachineReadableToDesk, 0.08335),
      SplitRatio(eeaMachineReadableToEGate, 0.7333),
      SplitRatio(eeaNonMachineReadableToDesk, 0.08335),
      SplitRatio(visaNationalToDesk, 0.05),
      SplitRatio(nonVisaNationalToDesk, 0.05)
    ))).toMap,
    terminalProcessingTimes = Map(
      T1 -> Map(
        b5jsskToDesk -> ProcTimesT1.b5jssk / 60,
        b5jsskChildToDesk -> ProcTimesT1.b5jssk / 60,
        eeaMachineReadableToDesk -> ProcTimesT1.eea / 60,
        eeaNonMachineReadableToDesk -> ProcTimesT1.eea / 60,
        eeaChildToDesk -> ProcTimesT1.eea / 60,
        gbrNationalToDesk -> ProcTimesT1.gbr / 60,
        gbrNationalChildToDesk -> ProcTimesT1.gbr / 60,
        b5jsskToEGate -> 38d / 60,
        eeaMachineReadableToEGate -> 44d / 60,
        gbrNationalToEgate -> 44d / 60,
        visaNationalToDesk -> ProcTimesT1.vn / 60,
        nonVisaNationalToDesk -> ProcTimesT1.nvn / 60,
      ),
      T2 -> Map(
        b5jsskToDesk -> ProcTimesT2.b5jssk / 60,
        b5jsskChildToDesk -> ProcTimesT2.b5jssk / 60,
        eeaMachineReadableToDesk -> ProcTimesT2.eea / 60,
        eeaNonMachineReadableToDesk -> ProcTimesT2.eea / 60,
        eeaChildToDesk -> ProcTimesT2.eea / 60,
        gbrNationalToDesk -> ProcTimesT2.gbr / 60,
        gbrNationalChildToDesk -> ProcTimesT2.gbr / 60,
        b5jsskToEGate -> 51d / 60,
        eeaMachineReadableToEGate -> 51d / 60,
        gbrNationalToEgate -> 51d / 60,
        visaNationalToDesk -> ProcTimesT2.vn / 60,
        nonVisaNationalToDesk -> ProcTimesT2.nvn / 60,
      ),
      T3 -> Map(
        b5jsskToDesk -> ProcTimesT3.b5jssk / 60,
        b5jsskChildToDesk -> ProcTimesT3.b5jssk / 60,
        eeaMachineReadableToDesk -> ProcTimesT3.eea / 60,
        eeaNonMachineReadableToDesk -> ProcTimesT3.eea / 60,
        eeaChildToDesk -> ProcTimesT3.eea / 60,
        gbrNationalToDesk -> ProcTimesT3.gbr / 60,
        gbrNationalChildToDesk -> ProcTimesT3.gbr / 60,
        b5jsskToEGate -> 44d / 60,
        eeaMachineReadableToEGate -> 44d / 60,
        gbrNationalToEgate -> 44d / 60,
        visaNationalToDesk -> ProcTimesT3.vn / 60,
        nonVisaNationalToDesk -> ProcTimesT3.nvn / 60,
      )),
    minMaxDesksByTerminalQueue24Hrs = Map(
      T1 -> Map(
        Queues.EGate -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)),
        Queues.EeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6)),
        Queues.NonEeaDesk -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 5, 6, 6, 6, 6, 6, 5, 5, 5, 6, 5, 5, 5, 5))
      ),
      T2 -> Map(
        Queues.EGate -> (List(1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
        Queues.EeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(8, 8, 8, 8, 8, 5, 5, 5, 5, 5, 5, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)),
        Queues.NonEeaDesk -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(3, 3, 3, 3, 3, 8, 8, 8, 8, 8, 8, 3, 3, 3, 3, 3, 6, 6, 6, 6, 3, 3, 3, 3))
      ),
      T3 -> Map(
        Queues.EGate -> (List(1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
        Queues.EeaDesk -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6)),
        Queues.NonEeaDesk -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3))
      )
    ),
    eGateBankSizes = Map(
      T1 -> Iterable(10),
      T2 -> Iterable(10),
      T3 -> Iterable(10),
    ),
    role = MAN,
    terminalPaxTypeQueueAllocation = Map(
      T1 -> (defaultQueueRatios + (EeaMachineReadable -> List(
        EGate -> 0.7968,
        EeaDesk -> (1.0 - 0.7968)
      ))),
      T2 -> (defaultQueueRatios + (EeaMachineReadable -> List(
        EGate -> 0.7140,
        EeaDesk -> (1.0 - 0.7140)
      ))),
      T3 -> (defaultQueueRatios + (EeaMachineReadable -> List(
        EGate -> 0.7038,
        EeaDesk -> (1.0 - 0.7038)
      )))),
    flexedQueues = Set(EeaDesk, NonEeaDesk),
    desksByTerminal = Map[Terminal, Int](
      T1 -> 14,
      T2 -> 11,
      T3 -> 9
    ),
    feedSources = Seq(ApiFeedSource, LiveBaseFeedSource, LiveFeedSource, AclFeedSource)
  )
}
