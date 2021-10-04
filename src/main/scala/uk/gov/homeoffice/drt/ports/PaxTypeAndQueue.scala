package uk.gov.homeoffice.drt.ports

import uk.gov.homeoffice.drt.Nationality
import uk.gov.homeoffice.drt.ports.Queues.Queue
import upickle.default._


object PaxTypesAndQueues {
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
  val transitToTransfer: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.Transit, Queues.Transfer)
  val nonVisaNationalToFastTrack: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.NonVisaNational, Queues.FastTrack)
  val undefinedToEea: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.UndefinedPaxType, Queues.EeaDesk)
  val undefinedToNonEea: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.UndefinedPaxType, Queues.NonEeaDesk)
  val undefinedToEgate: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.UndefinedPaxType, Queues.EGate)
  val undefinedToFastTrack: PaxTypeAndQueue = PaxTypeAndQueue(PaxTypes.UndefinedPaxType, Queues.FastTrack)

  def cedatDisplayName: Map[PaxTypeAndQueue, String] = Map(
    eeaMachineReadableToEGate -> "eGates",
    eeaMachineReadableToDesk -> "EEA (Machine Readable)",
    eeaChildToDesk -> "EEA child to Desk",
    eeaNonMachineReadableToDesk -> "EEA (Non Machine Readable)",
    b5jsskToDesk -> "B5JSSK to Desk",
    b5jsskChildToDesk -> "B5JSSK child to Desk",
    b5jsskToEGate -> "B5JSSK to eGates",
    visaNationalToDesk -> "Non EEA (Visa)",
    nonVisaNationalToDesk -> "Non EEA (Non Visa)",
    visaNationalToFastTrack -> "Fast Track (Visa)",
    nonVisaNationalToFastTrack -> "Fast Track (Non Visa)",
    transitToTransfer -> "Transfer",
    undefinedToEea -> "EEA (Undefined)",
    undefinedToNonEea -> "Non EEA (Undefined)",
    undefinedToEgate -> "eGates (Undefined)",
    undefinedToEgate -> "eGates (Undefined)",
  )

  val inOrder = List(
    eeaMachineReadableToEGate, eeaMachineReadableToDesk, eeaNonMachineReadableToDesk, visaNationalToDesk, nonVisaNationalToDesk, visaNationalToFastTrack, nonVisaNationalToFastTrack)
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
