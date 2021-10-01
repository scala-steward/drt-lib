package uk.gov.homeoffice.drt.ports

import ujson.Value.Value
import uk.gov.homeoffice.drt.ports.PaxTypes.{B5JPlusNational, B5JPlusNationalBelowEGateAge, EeaBelowEGateAge, EeaMachineReadable, EeaNonMachineReadable, NonVisaNational, Transit, UndefinedPaxType, VisaNational}
import upickle.default._

sealed trait PaxType {
  def name: String = getClass.getSimpleName

  def cleanName: String = getClass.getSimpleName.dropRight(1)
}

sealed trait EeaPaxType extends PaxType

sealed trait NonEeaPaxType extends PaxType

object PaxType {
  def apply(paxTypeString: String): PaxType = paxTypeString match {
    case "EeaMachineReadable$" => EeaMachineReadable
    case "EeaNonMachineReadable$" => EeaNonMachineReadable
    case "EeaBelowEGateAge$" => EeaBelowEGateAge
    case "VisaNational$" => VisaNational
    case "NonVisaNational$" => NonVisaNational
    case "B5JPlusNational$" => B5JPlusNational
    case "B5JPlusNationalBelowEGateAge$" => B5JPlusNationalBelowEGateAge
    case "Transit$" => Transit
    case _ => UndefinedPaxType
  }

  implicit val paxTypeReaderWriter: ReadWriter[PaxType] =
    readwriter[Value].bimap[PaxType](paxType => paxType.cleanName, (s: Value) => PaxType(s"${s.str}$$"))
}

object PaxTypes {

  case object EeaMachineReadable extends EeaPaxType

  case object EeaNonMachineReadable extends EeaPaxType

  case object EeaBelowEGateAge extends EeaPaxType

  case object VisaNational extends NonEeaPaxType

  case object NonVisaNational extends NonEeaPaxType

  case object B5JPlusNational extends NonEeaPaxType

  case object B5JPlusNationalBelowEGateAge extends NonEeaPaxType

  case object Transit extends PaxType

  case object UndefinedPaxType extends PaxType

  def displayName(pt: PaxType): String = pt match {
    case EeaMachineReadable => "EEA Machine Readable"
    case EeaNonMachineReadable => "EEA Non-Machine Readable"
    case EeaBelowEGateAge => "EEA Child"
    case VisaNational => "Visa National"
    case NonVisaNational => "Non-Visa National"
    case B5JPlusNational => "B5J+ National"
    case B5JPlusNationalBelowEGateAge => "B5J+ Child"
    case Transit => "Transit"
    case UndefinedPaxType => "Undefined"
    case other => other.name
  }

  def displayNameShort(pt: PaxType): String = pt match {
    case EeaMachineReadable => "EEA MR"
    case EeaNonMachineReadable => "EEA NMR"
    case EeaBelowEGateAge => "EEA U12"
    case VisaNational => "VN"
    case NonVisaNational => "NVN"
    case B5JPlusNational => "B5J+"
    case B5JPlusNationalBelowEGateAge => "B5J+ U12"
    case Transit => "Transit"
    case UndefinedPaxType => "Undefined"
    case other => other.name
  }
}
