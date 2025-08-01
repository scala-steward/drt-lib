package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.NQY
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues.{EeaDesk, NonEeaDesk, QueueDesk}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.ports.config.AirportConfigDefaults.{defaultPaxSplitsWithoutEgates, defaultProcessingTimes, defaultQueueRatiosWithoutEgates}
import uk.gov.homeoffice.drt.time.LocalDate

import scala.collection.immutable.SortedMap

object Nqy extends AirportConfigLike {

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("NQY"),
    portName = "Newquay",
    queuesByTerminal = SortedMap(LocalDate(2014, 1, 1) -> SortedMap(
      T1 -> Seq(QueueDesk)
    )),
    divertedQueues = Map(
      NonEeaDesk -> QueueDesk,
      EeaDesk -> QueueDesk
    ),
    slaByQueue = Map(
      QueueDesk -> 20,
    ),
    defaultWalkTimeMillis = Map(T1 -> 30000L),
    terminalPaxSplits = Map(T1 -> defaultPaxSplitsWithoutEgates),
    terminalProcessingTimes = Map(T1 -> defaultProcessingTimes),
    minMaxDesksByTerminalQueue24Hrs = Map(
      T1 -> Map(
        QueueDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2))
      )
    ),
    eGateBankSizes = Map(T1 -> Iterable()),
    role = NQY,
    terminalPaxTypeQueueAllocation = Map(T1 -> defaultQueueRatiosWithoutEgates),
    flexedQueues = Set(),
    desksByTerminal = Map(T1 -> 2),
    feedSources = Seq(ApiFeedSource, LiveFeedSource)
  )
}
