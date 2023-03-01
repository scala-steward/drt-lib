package uk.gov.homeoffice.drt.arrivals

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.actor.WalkTimeProvider
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2, Terminal}


class WalkTimeProviderSpec extends AnyWordSpec with Matchers {
  "A walk time provider" should {
    "read a gates csv" in {
      val walkTimeProvider = WalkTimeProvider(getClass.getClassLoader.getResource("gate-walk-times.csv").getPath)
      walkTimeProvider.walkTimes should===(Map[(Terminal, String), Int]((T1, "A1") -> 120))
    }
    "read a stands csv" in {
      val walkTimeProvider = WalkTimeProvider(getClass.getClassLoader.getResource("stand-walk-times.csv").getPath)
      walkTimeProvider.walkTimes should===(Map[(Terminal, String), Int]((T2, "A1a") -> 180))
    }
  }
}
