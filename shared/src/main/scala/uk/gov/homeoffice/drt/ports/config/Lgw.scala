package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.LGW
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._

import scala.collection.immutable.SortedMap

object Lgw extends AirportConfigLike {

  import AirportConfigDefaults._

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("LGW"),
    queuesByTerminal = SortedMap(
      N -> Seq(EeaDesk, EGate, NonEeaDesk),
      S -> Seq(EeaDesk, EGate, NonEeaDesk)
    ),
    slaByQueue = Map(
      EeaDesk -> 25,
      EGate -> 10,
      NonEeaDesk -> 45
    ),
    defaultWalkTimeMillis = Map(N -> 180000L, S -> 180000L),
    terminalPaxSplits = List(N, S).map(t => (t, SplitRatios(
      SplitSources.TerminalAverage,
      SplitRatio(eeaMachineReadableToDesk, 0.85 * 0.17),
      SplitRatio(eeaMachineReadableToEGate, 0.85 * 0.83),
      SplitRatio(eeaNonMachineReadableToDesk, 0d),
      SplitRatio(visaNationalToDesk, 0.06),
      SplitRatio(nonVisaNationalToDesk, 0.09)
    ))).toMap,
    terminalProcessingTimes = Map(
      N -> Map(
        b5jsskToDesk -> 57d / 60,
        b5jsskChildToDesk -> 57d / 60,
        eeaMachineReadableToDesk -> 39d / 60,
        eeaNonMachineReadableToDesk -> 39d / 60,
        eeaChildToDesk -> 39d / 60,
        b5jsskToEGate -> 47d / 60,
        eeaMachineReadableToEGate -> 47d / 60,
        visaNationalToDesk -> 125d / 60,
        nonVisaNationalToDesk -> 110d / 60
      ),
      S -> Map(
        b5jsskToDesk -> 63d / 60,
        b5jsskChildToDesk -> 63d / 60,
        eeaMachineReadableToDesk -> 40d / 60,
        eeaNonMachineReadableToDesk -> 40d / 60,
        eeaChildToDesk -> 40d / 60,
        b5jsskToEGate -> 47d / 60,
        eeaMachineReadableToEGate -> 47d / 60,
        visaNationalToDesk -> 125d / 60,
        nonVisaNationalToDesk -> 110d / 60,
      )),
    minMaxDesksByTerminalQueue24Hrs = Map(
      N -> Map(
        Queues.EGate -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)),
        Queues.EeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13)),
        Queues.NonEeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15))
      ),
      S -> Map(
        Queues.EGate -> (List(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)),
        Queues.EeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(15, 15, 15, 15, 15, 15, 13, 10, 10, 10, 10, 10, 10, 10, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13)),
        Queues.NonEeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(10, 10, 10, 10, 10, 10, 12, 15, 15, 15, 15, 15, 15, 15, 15, 13, 13, 13, 13, 13, 13, 13, 13, 13))
      )
    ),
    eGateBankSizes = Map(
      N -> Iterable(10, 10, 5),
      S -> Iterable(10, 10, 5)
    ),
    role = LGW,
    terminalPaxTypeQueueAllocation = Map(
      N -> (defaultQueueRatios + (EeaMachineReadable -> List(
        EGate -> 0.8244,
        EeaDesk -> (1.0 - 0.8244)
      ))),
      S -> (defaultQueueRatios + (EeaMachineReadable -> List(
        EGate -> 0.8375,
        EeaDesk -> (1.0 - 0.8375)
      )))),
    feedSources = Seq(ApiFeedSource, LiveBaseFeedSource, LiveFeedSource, ForecastFeedSource, AclFeedSource),
    flexedQueues = Set(EeaDesk, NonEeaDesk),
    desksByTerminal = Map[Terminal, Int](N -> 31, S -> 28)
  )
}
