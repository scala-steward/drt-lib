package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.LTN
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._

import scala.collection.immutable.SortedMap

object Ltn extends AirportConfigLike {

  import AirportConfigDefaults._

  private object ProcTimes {
    val gbr = 30.1
    val eea = 37.4
    val b5jssk = 52.9
    val nvn = 64.4
    val vn = 83.8
  }

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("LTN"),
    portName = "Luton",
    queuesByTerminal = SortedMap(
      T1 -> Seq(EeaDesk, EGate, NonEeaDesk)
    ),
    slaByQueue = defaultSlas,
    defaultWalkTimeMillis = Map(T1 -> 300000L),
    terminalPaxSplits = Map(T1 -> defaultPaxSplits),
    terminalProcessingTimes = Map(T1 -> Map(
      b5jsskToDesk -> ProcTimes.b5jssk / 60,
      b5jsskChildToDesk -> ProcTimes.b5jssk / 60,
      eeaMachineReadableToDesk -> ProcTimes.eea / 60,
      eeaNonMachineReadableToDesk -> ProcTimes.eea / 60,
      eeaChildToDesk -> ProcTimes.eea / 60,
      gbrNationalToDesk -> ProcTimes.gbr / 60,
      gbrNationalChildToDesk -> ProcTimes.gbr / 60,
      b5jsskToEGate -> 47d / 60,
      eeaMachineReadableToEGate -> 47d / 60,
      gbrNationalToEgate -> 47d / 60,
      visaNationalToDesk -> ProcTimes.vn / 60,
      nonVisaNationalToDesk -> ProcTimes.nvn / 60,
    )),
    minMaxDesksByTerminalQueue24Hrs = Map(
      T1 -> Map(
        Queues.EGate -> (List.fill(24)(1), List.fill(24)(2)),
        Queues.EeaDesk -> (List.fill(24)(1), List(9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9)),
        Queues.NonEeaDesk -> (List.fill(24)(1), List(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5))
      )
    ),
    eGateBankSizes = Map(T1 -> Iterable(10, 5)),
    hasEstChox = false,
    role = LTN,
    terminalPaxTypeQueueAllocation = Map(
      T1 -> (defaultQueueRatios + (EeaMachineReadable -> List(
        EGate -> 0.7922,
        EeaDesk -> (1.0 - 0.7922)
      )))
    ),
    flexedQueues = Set(EeaDesk, NonEeaDesk),
    desksByTerminal = Map(T1 -> 14),
    feedSources = Seq(ApiFeedSource, LiveBaseFeedSource, LiveFeedSource, AclFeedSource)
  )
}
