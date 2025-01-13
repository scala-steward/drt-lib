package uk.gov.homeoffice.drt.ports

import ujson.Value.Value
import uk.gov.homeoffice.drt.ports.PaxTypes._
import upickle.default._

sealed trait PaxType {
  def name: String = getClass.getSimpleName

  def cleanName: String = getClass.getSimpleName.dropRight(1)

  def id: Int
}

sealed trait GbrPaxType extends PaxType

sealed trait EeaPaxType extends PaxType

sealed trait NonEeaPaxType extends PaxType

object PaxType {
  def apply(paxTypeString: String): PaxType = paxTypeString match {
    case "GBRNational$" => GBRNational
    case "GBRNationalBelowEgateAge$" => GBRNationalBelowEgateAge
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

  def apply(paxTypeNumber: Int): PaxType = paxTypeNumber match {
    case 1 => GBRNational
    case 2 => GBRNationalBelowEgateAge
    case 3 => EeaMachineReadable
    case 4 => EeaNonMachineReadable
    case 5 => EeaBelowEGateAge
    case 6 => VisaNational
    case 7 => NonVisaNational
    case 8 => B5JPlusNational
    case 9 => B5JPlusNationalBelowEGateAge
    case 10 => Transit
    case _ => UndefinedPaxType
  }

  implicit val paxTypeReaderWriter: ReadWriter[PaxType] =
    readwriter[Value].bimap[PaxType](paxType => paxType.cleanName, (s: Value) => PaxType(s"${s.str}$$"))
}

object PaxTypes {
  case object GBRNational extends GbrPaxType {
    override def id: Int = 1
  }

  case object GBRNationalBelowEgateAge extends GbrPaxType {
    override def id: Int = 2
  }

  case object EeaMachineReadable extends EeaPaxType {
    override def id: Int = 3
  }

  case object EeaNonMachineReadable extends EeaPaxType {
    override def id: Int = 4
  }

  case object EeaBelowEGateAge extends EeaPaxType {
    override def id: Int = 5
  }

  case object VisaNational extends NonEeaPaxType {
    override def id: Int = 6
  }

  case object NonVisaNational extends NonEeaPaxType {
    override def id: Int = 7
  }

  case object B5JPlusNational extends NonEeaPaxType {
    override def id: Int = 8
  }

  case object B5JPlusNationalBelowEGateAge extends NonEeaPaxType {
    override def id: Int = 9
  }

  case object Transit extends PaxType {
    override def id: Int = 10
  }

  case object UndefinedPaxType extends PaxType {
    override def id: Int = -1
  }

  val allPaxTypes: Iterable[PaxType] = Iterable(
    GBRNational,
    GBRNationalBelowEgateAge,
    EeaMachineReadable,
    EeaNonMachineReadable,
    EeaBelowEGateAge,
    VisaNational,
    NonVisaNational,
    B5JPlusNational,
    B5JPlusNationalBelowEGateAge,
    Transit,
  )

  def displayName(pt: PaxType): String = pt match {
    case GBRNational => "GBR National"
    case GBRNationalBelowEgateAge => "GBR National Child"
    case EeaMachineReadable => "EEA Machine Readable"
    case EeaNonMachineReadable => "EEA Non-Machine Readable"
    case EeaBelowEGateAge => "EEA Child"
    case VisaNational => "Visa National"
    case NonVisaNational => "Non-Visa National"
    case B5JPlusNational => "B5J+ National"
    case B5JPlusNationalBelowEGateAge => "B5J+ Child"
    case Transit => "Transit"
    case other => other.name
  }

  def displayNameShort(pt: PaxType, isBeforeAgeEligibilityChangeDate: Boolean): String = pt match {
    case GBRNational => "GBR"
    case GBRNationalBelowEgateAge => if (isBeforeAgeEligibilityChangeDate) "GBR U12" else "GBR U10"
    case EeaMachineReadable => "EEA MR"
    case EeaNonMachineReadable => "EEA NMR"
    case EeaBelowEGateAge => if (isBeforeAgeEligibilityChangeDate) "EEA U12" else "EEA U10"
    case VisaNational => "VN"
    case NonVisaNational => "NVN"
    case B5JPlusNational => "B5J+"
    case B5JPlusNationalBelowEGateAge => if (isBeforeAgeEligibilityChangeDate) "B5J+ U12" else "B5J+ U10"
    case Transit => "Transit"
    case other => other.name
  }
}
