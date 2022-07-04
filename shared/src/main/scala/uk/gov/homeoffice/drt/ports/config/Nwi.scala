package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.NWI
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues.{eeaMachineReadableToDesk, eeaNonMachineReadableToDesk, nonVisaNationalToDesk, visaNationalToDesk}
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._

import scala.collection.immutable.SortedMap
import scala.concurrent.duration.DurationDouble

object Nwi extends AirportConfigLike {

  import AirportConfigDefaults._

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("NWI"),
    queuesByTerminal = SortedMap(
      T1 -> Seq(Queues.EeaDesk, Queues.NonEeaDesk)
    ),
    slaByQueue = Map(
      Queues.EeaDesk -> 25,
      Queues.NonEeaDesk -> 45
    ),
    defaultWalkTimeMillis = Map(T1 -> 2.5d.minutes.toMillis),
    terminalPaxSplits = Map(T1 -> defaultPaxSplitsWithoutEgates),
    terminalProcessingTimes = Map(T1 -> defaultProcessingTimes),
    minMaxDesksByTerminalQueue24Hrs = Map(T1 -> Map(
      Queues.EeaDesk -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)),
      Queues.NonEeaDesk -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3))
    )),
    eGateBankSizes = Map(),
    role = NWI,
    terminalPaxTypeQueueAllocation = Map(T1 -> defaultQueueRatiosWithoutEgates),
    feedSources = Seq(ApiFeedSource, LiveFeedSource),
    flexedQueues = Set(EeaDesk, NonEeaDesk),
    desksByTerminal = Map(T1 -> 3)
  )
}
