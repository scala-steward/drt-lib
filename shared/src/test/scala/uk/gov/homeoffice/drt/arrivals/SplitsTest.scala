package uk.gov.homeoffice.drt.arrivals

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.Nationality
import uk.gov.homeoffice.drt.arrivals.EventTypes.{DC, InvalidEventType}
import uk.gov.homeoffice.drt.arrivals.SplitStyle.PaxNumbers
import uk.gov.homeoffice.drt.ports.PaxTypes.GBRNational
import uk.gov.homeoffice.drt.ports.Queues.EeaDesk
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources
import uk.gov.homeoffice.drt.ports.{ApiPaxTypeAndQueueCount, PaxAge}
import upickle.default.{read, writeJs}

class SplitsTest extends AnyWordSpec with Matchers {
  "Splits" should {
    "serialise to json and back" in {
      val splits = Splits(
        Set(ApiPaxTypeAndQueueCount(GBRNational, EeaDesk, 1, Option(Map(Nationality("GBR") -> 1)), Option(Map(PaxAge(25) -> 1)))),
        SplitSources.Historical,
        Option(DC),
        PaxNumbers
      )
      val serialised = writeJs(splits)
      val deserialised = read[Splits](serialised)

      deserialised shouldBe splits
    }

    "serialise to json and back when there's an InvalidEventType" in {
      val splits = Splits(
        Set(ApiPaxTypeAndQueueCount(GBRNational, EeaDesk, 1, Option(Map(Nationality("GBR") -> 1)), Option(Map(PaxAge(25) -> 1)))),
        SplitSources.Historical,
        Option(InvalidEventType),
        PaxNumbers
      )
      val serialised = writeJs(splits)
      val deserialised = read[Splits](serialised)

      deserialised shouldBe splits
    }
  }
}
