package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.PIK
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._

import scala.collection.immutable.SortedMap

object Pik extends AirportConfigLike {

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("PIK"),
    queuesByTerminal = SortedMap(
      T1 -> Seq(Queues.QueueDesk)
    ),
    divertedQueues = Map(
      Queues.NonEeaDesk -> Queues.QueueDesk,
      Queues.EeaDesk -> Queues.QueueDesk
    ),
    slaByQueue = Map(
      Queues.QueueDesk -> 20,
    ),
    defaultWalkTimeMillis = Map(T1 -> 780000L),
    terminalPaxSplits = Map(T1 -> SplitRatios(
      SplitSources.TerminalAverage,
      SplitRatio(eeaMachineReadableToDesk, 0.99 * 0.2),
      SplitRatio(eeaNonMachineReadableToDesk, 0),
      SplitRatio(visaNationalToDesk, 0.0),
      SplitRatio(nonVisaNationalToDesk, 0.01)
    )),
    terminalProcessingTimes = Map(T1 -> Map(
      eeaMachineReadableToDesk -> 20d / 60,
      eeaNonMachineReadableToDesk -> 50d / 60,
      visaNationalToDesk -> 100d / 60,
      nonVisaNationalToDesk -> 80d / 60
    )),
    minMaxDesksByTerminalQueue24Hrs = Map(
      T1 -> Map(
        Queues.QueueDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5))
      )
    ),
    eGateBankSizes = Map(T1 -> Iterable()),
    role = PIK,
    terminalPaxTypeQueueAllocation = Map(
      T1 -> Map(
        EeaMachineReadable -> List(Queues.QueueDesk -> 1.0),
        EeaBelowEGateAge -> List(Queues.QueueDesk -> 1.0),
        EeaNonMachineReadable -> List(Queues.QueueDesk -> 1.0),
        NonVisaNational -> List(Queues.QueueDesk -> 1.0),
        VisaNational -> List(Queues.QueueDesk -> 1.0),
        B5JPlusNational -> List(Queues.QueueDesk -> 1.0),
        B5JPlusNationalBelowEGateAge -> List(Queues.QueueDesk -> 1.0)
      )),
    flexedQueues = Set(),
    desksByTerminal = Map(T1 -> 5),
    feedSources = Seq(ApiFeedSource, LiveBaseFeedSource, LiveFeedSource, AclFeedSource)
  )
}
