package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.GLA
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.time.LocalDate

import scala.collection.immutable.SortedMap

object Gla extends AirportConfigLike {

  import AirportConfigDefaults._

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("GLA"),
    portName = "Glasgow",
    queuesByTerminal = SortedMap(LocalDate(2014, 1, 1) -> SortedMap(
      T1 -> Seq(EeaDesk, EGate, NonEeaDesk)
    )),
    slaByQueue = Map(
      EeaDesk -> 25,
      NonEeaDesk -> 45,
      EGate -> 25
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
        EGate -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
        EeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7)),
        NonEeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7))
      )
    ),
    eGateBankSizes = Map(T1 -> Iterable(5)),
    role = GLA,
    terminalPaxTypeQueueAllocation = Map(
      T1 -> (defaultQueueRatios + (EeaMachineReadable -> List(
        EGate -> 0.6993,
        EeaDesk -> (1.0 - 0.6993)
      )))
    ),
    flexedQueues = Set(EeaDesk, NonEeaDesk),
    desksByTerminal = Map(T1 -> 7),
    feedSources = Seq(ApiFeedSource, LiveBaseFeedSource, LiveFeedSource, AclFeedSource)
  )
}
