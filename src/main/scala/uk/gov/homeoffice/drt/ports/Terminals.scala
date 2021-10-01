package uk.gov.homeoffice.drt.ports

import upickle.default._

trait ClassNameForToString {
  override val toString: String = getClass.toString.split("\\$").last
}

object Terminals {

  sealed trait Terminal extends ClassNameForToString with Ordered[Terminal] {
    override def compare(that: Terminal): Int = toString.compare(that.toString)
  }

  object Terminal {
    implicit val rw: ReadWriter[Terminal] = macroRW

    def apply(terminalName: String): Terminal = terminalName.toLowerCase match {
      case "t1" => T1
      case "t2" => T2
      case "t3" => T3
      case "t4" => T4
      case "t5" => T5
      case "a1" => A1
      case "a2" => A2
      case "1i" => T1
      case "2i" => T2
      case "1d" => T1
      case "2d" => T2
      case "5d" => T5
      case "3i" => T3
      case "4i" => T4
      case "5i" => T5
      case "ter" => T1
      case "n" => N
      case "s" => S
      case "mt" => T1
      case "cta" => CTA
      case "mainapron" => MainApron
      case _ => InvalidTerminal
    }
  }

  case object InvalidTerminal extends Terminal {
    override val toString: String = ""
  }

  case object T1 extends Terminal

  case object T2 extends Terminal

  case object T3 extends Terminal

  case object T4 extends Terminal

  case object T5 extends Terminal

  case object A1 extends Terminal

  case object A2 extends Terminal

  case object ACLTER extends Terminal

  case object N extends Terminal

  case object S extends Terminal

  case object MainApron extends Terminal

  case object CTA extends Terminal

}
