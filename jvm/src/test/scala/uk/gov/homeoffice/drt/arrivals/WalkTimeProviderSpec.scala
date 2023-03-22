package uk.gov.homeoffice.drt.arrivals

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.actor.WalkTimeProvider
import uk.gov.homeoffice.drt.ports.Terminals.T1


class WalkTimeProviderSpec extends AnyWordSpec with Matchers {
  val gatesCsvPath: String = getClass.getClassLoader.getResource("gate-walk-times.csv").getPath
  val standsCsvPath: String = getClass.getClassLoader.getResource("stand-walk-times.csv").getPath

  "A walk time provider" should {
    "throw an exception if the gates file doesn't exist" in {
      assertThrows[Exception] {
        WalkTimeProvider(Option("non-existent-file"), None)
      }
    }
    "throw an exception if the stands file is empty" in {
      assertThrows[Exception] {
        WalkTimeProvider(None, Option("non-existent-file"))
      }
    }

    val gateA1T1WalkTime = 120
    val standA1aT1WalkTime = 180
    val gatesOnlyProvider = WalkTimeProvider(Option(gatesCsvPath), None)
    val standsOnlyProvider = WalkTimeProvider(None, Option(standsCsvPath))
    val gatesAndStandsProvider = WalkTimeProvider(Option(gatesCsvPath), Option(standsCsvPath))

    "read a gates csv" in {
      gatesOnlyProvider(T1, "A1", "") should===(Option(gateA1T1WalkTime))
    }
    "read a stands csv" in {
      standsOnlyProvider(T1, "", "A1a") should===(Option(standA1aT1WalkTime))
    }
    "prioritise stands over gates" in {
      gatesAndStandsProvider(T1, "A1", "A1a") should===(Option(standA1aT1WalkTime))
    }
    "find the gate where it exists and the stand does not" in {
      gatesAndStandsProvider(T1, "A1", "X") should ===(Option(gateA1T1WalkTime))
    }
    "find the stand where it exists and the gate does not" in {
      gatesAndStandsProvider(T1, "X", "A1a") should===(Option(standA1aT1WalkTime))
    }
  }
}
