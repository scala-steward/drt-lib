package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.{HUY, LBA}
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._

import scala.collection.immutable.SortedMap
import scala.concurrent.duration.DurationInt

object Huy extends AirportConfigLike {

  import AirportConfigDefaults._

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("HUY"),
    queuesByTerminal = SortedMap(
      T1 -> Seq(Queues.EeaDesk, Queues.NonEeaDesk)
    ),
    slaByQueue = Map(
      Queues.EeaDesk -> 25,
      Queues.NonEeaDesk -> 45
    ),
    defaultWalkTimeMillis = Map(T1 -> 5.minutes.toMillis),
    terminalPaxSplits = List(T1).map(t => (t, defaultPaxSplits)).toMap,
    terminalProcessingTimes = Map(T1 -> defaultProcessingTimes),
    minMaxDesksByTerminalQueue24Hrs = Map(T1 -> Map(
      Queues.EeaDesk -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
      Queues.NonEeaDesk -> (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
    )),
    eGateBankSizes = Map(),
    role = HUY,
    terminalPaxTypeQueueAllocation = Map(
      T1 -> Map(
        EeaMachineReadable -> List(Queues.EeaDesk -> 1.0),
        EeaBelowEGateAge -> List(Queues.EeaDesk -> 1.0),
        EeaNonMachineReadable -> List(Queues.EeaDesk -> 1.0),
        NonVisaNational -> List(Queues.NonEeaDesk -> 1.0),
        VisaNational -> List(Queues.NonEeaDesk -> 1.0),
        B5JPlusNational -> List(Queues.EeaDesk -> 1.0),
        B5JPlusNationalBelowEGateAge -> List(Queues.EeaDesk -> 1.0)
      )),
    feedSources = Seq(ApiFeedSource, LiveFeedSource),
    flexedQueues = Set(EeaDesk, NonEeaDesk),
    desksByTerminal = Map(T1 -> 3)
  )
}
