package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.BRS
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._

import scala.collection.immutable.SortedMap

object Brs extends AirportConfigLike {

  import AirportConfigDefaults._

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("BRS"),
    queuesByTerminal = SortedMap(
      T1 -> Seq(Queues.EeaDesk, Queues.NonEeaDesk, Queues.EGate)
    ),
    slaByQueue = Map(
      Queues.EeaDesk -> 25,
      Queues.NonEeaDesk -> 45,
      Queues.EGate -> 25
    ),
    defaultWalkTimeMillis = Map(T1 -> 780000L),
    terminalPaxSplits = Map(T1 -> SplitRatios(
      SplitSources.TerminalAverage,
      SplitRatio(eeaMachineReadableToDesk, 0.99 * 0.2),
      SplitRatio(eeaMachineReadableToEGate, 0.99 * 0.8),
      SplitRatio(eeaNonMachineReadableToDesk, 0),
      SplitRatio(visaNationalToDesk, 0.0),
      SplitRatio(nonVisaNationalToDesk, 0.01)
    )),
    terminalProcessingTimes = Map(T1 -> defaultProcessingTimes),
    minMaxDesksByTerminalQueue24Hrs = Map(
      T1 -> Map(
        Queues.EGate -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
        Queues.EeaDesk -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5)),
        Queues.NonEeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5))
      )
    ),
    eGateBankSizes = Map(T1 -> Iterable(5, 5)),
    role = BRS,
    terminalPaxTypeQueueAllocation = Map(
      T1 -> (
        defaultQueueRatios +
          (EeaMachineReadable -> List(EGate -> 0.7742, EeaDesk -> (1.0 - 0.7742))),
        )
    ),
    feedSources = Seq(ApiFeedSource, LiveBaseFeedSource, AclFeedSource),
    flexedQueues = Set(EeaDesk, NonEeaDesk),
    desksByTerminal = Map(T1 -> 9)
  )
}
