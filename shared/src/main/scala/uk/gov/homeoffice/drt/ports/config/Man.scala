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

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("MAN"),
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
    terminalProcessingTimes = Map(T1 -> Map(
      b5jsskToDesk -> (53d / 60) * b5jScalingFactor,
      b5jsskChildToDesk -> (53d / 60) * b5jScalingFactor,
      eeaMachineReadableToDesk -> 38d / 60,
      eeaNonMachineReadableToDesk -> 38d / 60,
      eeaChildToDesk -> 38d / 60,
      gbrNationalToDesk -> 29d / 60,
      gbrNationalChildToDesk -> 29d / 60,
      b5jsskToEGate -> (38d / 60) * b5jScalingFactor,
      eeaMachineReadableToEGate -> 44d / 60,
      gbrNationalToEgate -> 44d / 60,
      visaNationalToDesk -> 109d / 60,
      nonVisaNationalToDesk -> 79d / 60,
    ), T2 -> Map(
      b5jsskToDesk -> (57d / 60) * b5jScalingFactor,
      b5jsskChildToDesk -> (57d / 60) * b5jScalingFactor,
      eeaMachineReadableToDesk -> 42d / 60,
      eeaNonMachineReadableToDesk -> 42d / 60,
      eeaChildToDesk -> 42d / 60,
      gbrNationalToDesk -> 30d / 60,
      gbrNationalChildToDesk -> 30d / 60,
      b5jsskToEGate -> (51d / 60) * b5jScalingFactor,
      eeaMachineReadableToEGate -> 51d / 60,
      gbrNationalToEgate -> 51d / 60,
      visaNationalToDesk -> 105d / 60,
      nonVisaNationalToDesk -> 92d / 60,
    ), T3 -> Map(
      b5jsskToDesk -> (56d / 60) * b5jScalingFactor,
      b5jsskChildToDesk -> (56d / 60) * b5jScalingFactor,
      eeaMachineReadableToDesk -> 38d / 60,
      eeaNonMachineReadableToDesk -> 38d / 60,
      eeaChildToDesk -> 38d / 60,
      gbrNationalToDesk -> 29d / 60,
      gbrNationalChildToDesk -> 29d / 60,
      b5jsskToEGate -> (44d / 60) * b5jScalingFactor,
      eeaMachineReadableToEGate -> 44d / 60,
      gbrNationalToEgate -> 44d / 60,
      visaNationalToDesk -> 111d / 60,
      nonVisaNationalToDesk -> 92d / 60,
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
