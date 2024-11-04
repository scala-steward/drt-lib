package uk.gov.homeoffice.drt.arrivals

import ujson.Value.Value
import upickle.default.{ReadWriter, readwriter}

sealed trait SplitStyle {
  def name: String = getClass.getSimpleName

  def id: Int
}

object SplitStyle {
  def apply(splitStyle: String): SplitStyle = splitStyle match {
    case "PaxNumbers$" => PaxNumbers
    case "PaxNumbers" => PaxNumbers
    case "Percentage$" => Percentage
    case "Percentage" => Percentage
    case "Ratio" => Ratio
    case _ => UndefinedSplitStyle
  }

  def apply(id: Int): SplitStyle = id match {
    case 1 => PaxNumbers
    case 2 => Percentage
    case 3 => Ratio
    case _ => UndefinedSplitStyle
  }

  implicit val splitStyleReadWriter: ReadWriter[SplitStyle] =
    readwriter[Value].bimap[SplitStyle](
      feedSource => feedSource.toString,
      (s: Value) => apply(s.str)
    )

  case object PaxNumbers extends SplitStyle {
    override def id: Int = 1
  }

  case object Percentage extends SplitStyle {
    override def id: Int = 2
  }

  case object Ratio extends SplitStyle {
    override def id: Int = 3
  }

  case object UndefinedSplitStyle extends SplitStyle {
    override def id: Int = -1
  }
}
