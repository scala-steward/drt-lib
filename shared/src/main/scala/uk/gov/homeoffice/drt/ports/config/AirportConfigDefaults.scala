package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk, NonEeaDesk, Queue}
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports._

object AirportConfigDefaults {
  val defaultSlas: Map[Queue, Int] = Map(
    EeaDesk -> 20,
    EGate -> 25,
    NonEeaDesk -> 45
  )

  val defaultPaxSplits: SplitRatios = SplitRatios(
    SplitSources.TerminalAverage,
    SplitRatio(eeaMachineReadableToDesk, 0.1625),
    SplitRatio(eeaMachineReadableToEGate, 0.4875),
    SplitRatio(eeaNonMachineReadableToDesk, 0.1625),
    SplitRatio(visaNationalToDesk, 0.05),
    SplitRatio(nonVisaNationalToDesk, 0.05)
  )

  val defaultQueueRatios: Map[PaxType, Seq[(Queue, Double)]] = Map(
    EeaMachineReadable -> List(Queues.EGate -> 0.8, Queues.EeaDesk -> 0.2),
    EeaBelowEGateAge -> List(Queues.EeaDesk -> 1.0),
    EeaNonMachineReadable -> List(Queues.EeaDesk -> 1.0),
    NonVisaNational -> List(Queues.NonEeaDesk -> 1.0),
    VisaNational -> List(Queues.NonEeaDesk -> 1.0),
    B5JPlusNational -> List(Queues.EGate -> 0.6, Queues.EeaDesk -> 0.4),
    B5JPlusNationalBelowEGateAge -> List(Queues.EeaDesk -> 1)
  )

  val defaultQueueRatiosWithoutEgates: Map[PaxType, Seq[(Queue, Double)]] = defaultQueueRatios + (
    EeaMachineReadable -> List(EeaDesk -> 1.0),
    B5JPlusNational -> List(EeaDesk -> 1.0),
  )

  val defaultProcessingTimes: Map[PaxTypeAndQueue, Double] = Map(
    b5jsskToDesk -> 55d / 60,
    b5jsskChildToDesk -> 55d / 60,
    eeaChildToDesk -> 38d / 60,
    eeaMachineReadableToDesk -> 38d / 60,
    eeaNonMachineReadableToDesk -> 38d / 60,
    b5jsskToEGate -> 35d / 60,
    eeaMachineReadableToEGate -> 35d / 60,
    visaNationalToDesk -> 109d / 60,
    nonVisaNationalToDesk -> 87d / 60
  )
}
