package uk.gov.homeoffice.drt.arrivals

import upickle.default.{ReadWriter, macroRW}
import scala.util.{Failure, Success, Try}

object VoyageNumberLike {
  implicit val rw: ReadWriter[VoyageNumberLike] = ReadWriter.merge(VoyageNumber.rw, macroRW[InvalidVoyageNumber.type])
}

sealed trait VoyageNumberLike {
  def numeric: Int

  def toPaddedString: String
}

case class VoyageNumber(numeric: Int) extends VoyageNumberLike with Ordered[VoyageNumber] {
  override def toString: String = numeric.toString

  def toPaddedString: String = {
    val string = numeric.toString
    val prefix = string.length match {
      case 4 => ""
      case 3 => "0"
      case 2 => "00"
      case 1 => "000"
      case _ => ""
    }
    prefix + string
  }

  override def compare(that: VoyageNumber): Int = numeric.compare(that.numeric)
}

case object InvalidVoyageNumber extends VoyageNumberLike {
  override def toString: String = "invalid"

  override def toPaddedString: String = toString

  override def numeric: Int = 0
}

case object VoyageNumber {
  implicit val rw: ReadWriter[VoyageNumber] = macroRW

  def apply(string: String): VoyageNumberLike = Try(string.toInt) match {
    case Success(value) => VoyageNumber(value)
    case Failure(_) => InvalidVoyageNumber
  }
}
