package uk.gov.homeoffice.drt.ports

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.Terminals._

class TerminalsTest extends AnyWordSpec with Matchers {
  "Terminals " should {
    "be correctly mapped from their name strings" in {
      val nameToTerminal = Map(
        "t1" -> T1,
        "t2" -> T2,
        "t3" -> T3,
        "t4" -> T4,
        "t5" -> T5,
        "a1" -> A1,
        "a2" -> A2,
        "1i" -> T1,
        "2i" -> T2,
        "1d" -> T1,
        "2d" -> T2,
        "5d" -> T5,
        "3i" -> T3,
        "4i" -> T4,
        "5i" -> T5,
        "ter" -> T1,
        "1" -> T1,
        "n" -> N,
        "s" -> S,
        "mt" -> T1,
        "cta" -> CTA,
        "mainapron" -> MainApron,
      )
      nameToTerminal.map { case (name, expectedTerminal) =>
        Terminal(name) should ===(expectedTerminal)
      }
    }

    "return correct number strings for terminals" in {
      val terminalToNumberString = Map(
        T1 -> "1",
        T2 -> "2",
        T3 -> "3",
        T4 -> "4",
        T5 -> "5",
        A1 -> "A1",
        A2 -> "A2",
        ACLTER -> "ACLTER",
        N -> "N",
        S -> "S",
        MainApron -> "MainApron",
        CTA -> "CTA",
        InvalidTerminal -> ""
      )

      terminalToNumberString.map { case (terminal, expectedNumberString) =>
        Terminal.numberString(terminal) should ===(expectedNumberString)
      }
    }
  }

}
