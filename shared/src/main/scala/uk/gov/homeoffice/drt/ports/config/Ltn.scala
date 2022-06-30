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

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("LTN"),
    queuesByTerminal = SortedMap(
      T1 -> Seq(EeaDesk, EGate, NonEeaDesk)
    ),
    slaByQueue = defaultSlas,
    defaultWalkTimeMillis = Map(T1 -> 300000L),
    terminalPaxSplits = Map(T1 -> defaultPaxSplits),
    terminalProcessingTimes = Map(T1 -> Map(
      b5jsskToDesk -> 0, //59d / 60,
      b5jsskChildToDesk -> 0, //59d / 60,
      eeaMachineReadableToDesk -> 37d / 60,
      eeaNonMachineReadableToDesk -> 37d / 60,
      eeaChildToDesk -> 37d / 60,
      gbrNationalToDesk -> 29d / 60,
      gbrNationalChildToDesk -> 29d / 60,
      b5jsskToEGate -> 0, //47d / 60,
      eeaMachineReadableToEGate -> 47d / 60,
      gbrNationalToEgate -> 47d / 60,
      visaNationalToDesk -> 88d / 60,
      nonVisaNationalToDesk -> 53d / 60,
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
