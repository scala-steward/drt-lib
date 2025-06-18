package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.PIK
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues.{EeaDesk, NonEeaDesk, QueueDesk}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.ports.config.AirportConfigDefaults.{defaultPaxSplitsWithoutEgates, defaultProcessingTimes, defaultQueueRatiosWithoutEgates}
import uk.gov.homeoffice.drt.time.LocalDate

import scala.collection.immutable.SortedMap

object Pik extends AirportConfigLike {

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("PIK"),
    portName = "Glasgow Prestwick",
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
    defaultWalkTimeMillis = Map(T1 -> 780000L),
    terminalPaxSplits = Map(T1 -> defaultPaxSplitsWithoutEgates),
    terminalProcessingTimes = Map(T1 -> defaultProcessingTimes),
    minMaxDesksByTerminalQueue24Hrs = Map(
      T1 -> Map(
        QueueDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), List(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5))
      )
    ),
    eGateBankSizes = Map(T1 -> Iterable()),
    role = PIK,
    terminalPaxTypeQueueAllocation = Map(T1 -> defaultQueueRatiosWithoutEgates),
    flexedQueues = Set(),
    desksByTerminal = Map(T1 -> 5),
    feedSources = Seq(ApiFeedSource, LiveFeedSource)
  )
}
