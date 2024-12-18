package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.EDI
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._

import scala.collection.immutable.SortedMap

object Edi extends AirportConfigLike {

  import AirportConfigDefaults._

  private object ProcTimesA1 {
    val gbr = 26.0
    val eea = 30.9
    val b5jssk = 42.6
    val nvn = 68.5
    val vn = 86.1
  }

  private object ProcTimesA2 {
    val gbr = 29.7
    val eea = 35.9
    val b5jssk = 49.9
    val nvn = 77.3
    val vn = 92.9
  }

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("EDI"),
    portName = "Edinburgh",
    queuesByTerminal = SortedMap(
      A1 -> Seq(EeaDesk, EGate, NonEeaDesk),
      A2 -> Seq(EeaDesk, EGate, NonEeaDesk)
    ),
    slaByQueue = defaultSlas,
    defaultWalkTimeMillis = Map(A1 -> 180000L, A2 -> 120000L),
    terminalPaxSplits = List(A1, A2).map(t => (t, defaultPaxSplits)).toMap,
    terminalProcessingTimes = Map(
      A1 -> Map(
        b5jsskToDesk -> ProcTimesA1.b5jssk / 60,
        b5jsskChildToDesk -> ProcTimesA1.b5jssk / 60,
        eeaMachineReadableToDesk -> ProcTimesA1.eea / 60,
        eeaNonMachineReadableToDesk -> ProcTimesA1.eea / 60,
        eeaChildToDesk -> ProcTimesA1.eea / 60,
        gbrNationalToDesk -> ProcTimesA1.gbr / 60,
        gbrNationalChildToDesk -> ProcTimesA1.gbr / 60,
        b5jsskToEGate -> 47d / 60,
        eeaMachineReadableToEGate -> 47d / 60,
        gbrNationalToEgate -> 47d / 60,
        visaNationalToDesk -> ProcTimesA1.vn / 60,
        nonVisaNationalToDesk -> ProcTimesA1.nvn / 60
      ),
      A2 -> Map(
        b5jsskToDesk -> ProcTimesA2.b5jssk / 60,
        b5jsskChildToDesk -> ProcTimesA2.b5jssk / 60,
        eeaMachineReadableToDesk -> ProcTimesA2.eea / 60,
        eeaNonMachineReadableToDesk -> ProcTimesA2.eea / 60,
        eeaChildToDesk -> ProcTimesA2.eea / 60,
        gbrNationalToDesk -> ProcTimesA2.gbr / 60,
        gbrNationalChildToDesk -> ProcTimesA2.gbr / 60,
        b5jsskToEGate -> 47d / 60,
        eeaMachineReadableToEGate -> 47d / 60,
        gbrNationalToEgate -> 47d / 60,
        visaNationalToDesk -> ProcTimesA2.vn / 60,
        nonVisaNationalToDesk -> ProcTimesA2.nvn / 60,
      )),
    minMaxDesksByTerminalQueue24Hrs = Map(
      A1 -> Map(
        Queues.EGate -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
        Queues.EeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9)),
        Queues.NonEeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(2, 2, 2, 2, 2, 2, 6, 6, 3, 3, 3, 3, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3))
      ),
      A2 -> Map(
        Queues.EGate -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
        Queues.EeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6)),
        Queues.NonEeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3))
      )
    ),
    eGateBankSizes = Map(A1 -> Iterable(5), A2 -> Iterable(10)),
    role = EDI,
    terminalPaxTypeQueueAllocation = Map(
      A1 -> (defaultQueueRatios + (EeaMachineReadable -> List(
        EGate -> 0.8140,
        EeaDesk -> (1.0 - 0.8140)
      ))),
      A2 -> (defaultQueueRatios + (EeaMachineReadable -> List(
        EGate -> 0.7894,
        EeaDesk -> (1.0 - 0.7894)
      )))
    ),
    flexedQueues = Set(EeaDesk, NonEeaDesk),
    desksByTerminal = Map[Terminal, Int](A1 -> 11, A2 -> 9),
    feedSources = Seq(ApiFeedSource, LiveBaseFeedSource, LiveFeedSource, AclFeedSource),
    feedSourceMonitorExemptions = Seq(LiveFeedSource),
    hasEstChox = false
  )
}
