package uk.gov.homeoffice.drt.ports

import uk.gov.homeoffice.drt.Nationality
import uk.gov.homeoffice.drt.ports.Queues.Queue
import upickle.default._


object PaxTypesAndQueues {
  val gbrNationalToEgate: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.GBRNational, Queues.EGate)
  val gbrNationalToDesk: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.GBRNational, Queues.EeaDesk)
  val gbrNationalChildToDesk: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.GBRNationalBelowEgateAge, Queues.EeaDesk)
  val eeaMachineReadableToDesk: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.EeaMachineReadable, Queues.EeaDesk)
  val eeaChildToDesk: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.EeaBelowEGateAge, Queues.EeaDesk)
  val eeaMachineReadableToEGate: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.EeaMachineReadable, Queues.EGate)
  val eeaNonMachineReadableToDesk: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.EeaNonMachineReadable, Queues.EeaDesk)
  val b5jsskToDesk: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.B5JPlusNational, Queues.EeaDesk)
  val b5jsskChildToDesk: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.B5JPlusNationalBelowEGateAge, Queues.EeaDesk)
  val b5jsskToEGate: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.B5JPlusNational, Queues.EGate)
  val visaNationalToDesk: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.VisaNational, Queues.NonEeaDesk)
  val nonVisaNationalToDesk: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.NonVisaNational, Queues.NonEeaDesk)
  val visaNationalToFastTrack: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.VisaNational, Queues.FastTrack)
  val nonVisaNationalToFastTrack: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.NonVisaNational, Queues.FastTrack)
  val transitToTransfer: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.Transit, Queues.Transfer)

  val allPaxTypeAndQueues = Set(
    gbrNationalToEgate,
    gbrNationalToDesk,
    gbrNationalChildToDesk,
    eeaMachineReadableToDesk,
    eeaChildToDesk,
    eeaMachineReadableToEGate,
    eeaNonMachineReadableToDesk,
    b5jsskToDesk,
    b5jsskChildToDesk,
    b5jsskToEGate,
    visaNationalToDesk,
    nonVisaNationalToDesk,
    visaNationalToFastTrack,
    nonVisaNationalToFastTrack,
    transitToTransfer,
  )

  val inOrder = List(
    eeaMachineReadableToEGate,
    eeaMachineReadableToDesk,
    eeaNonMachineReadableToDesk,
    eeaChildToDesk,
    gbrNationalToEgate,
    gbrNationalToDesk,
    gbrNationalChildToDesk,
    b5jsskToEGate,
    b5jsskToDesk,
    b5jsskChildToDesk,
    visaNationalToDesk,
    nonVisaNationalToDesk,
    visaNationalToFastTrack,
    nonVisaNationalToFastTrack,
  )
}

case class PaxTypeAndQueue(passengerType: PaxType, queueType: Queue) {
  def key = s"${passengerType}_$queueType"

  def displayName = s"${PaxTypes.displayName(passengerType)} to ${Queues.displayName(queueType)}"
}

object PaxTypeAndQueue {
  def apply(split: ApiPaxTypeAndQueueCount): PaxTypeAndQueue = PaxTypeAndQueue(split.passengerType, split.queueType)

  implicit val rw: ReadWriter[PaxTypeAndQueue] = macroRW
}

case class ApiPaxTypeAndQueueCount(
                                    passengerType: PaxType,
                                    queueType: Queue,
                                    paxCount: Double,
                                    nationalities: Option[Map[Nationality, Double]],
                                    ages: Option[Map[PaxAge, Double]]
                                  ) {
  val paxTypeAndQueue: PaxTypeAndQueue = PaxTypeAndQueue(passengerType, queueType)
}

object ApiPaxTypeAndQueueCount {
  implicit val rw: ReadWriter[ApiPaxTypeAndQueueCount] = macroRW
}

case class PaxAge(years: Int) {
  def isUnder(age: Int): Boolean = years < age

  override def toString: String = s"$years"
}

object PaxAge {
  implicit val rw: ReadWriter[PaxAge] = macroRW
}
