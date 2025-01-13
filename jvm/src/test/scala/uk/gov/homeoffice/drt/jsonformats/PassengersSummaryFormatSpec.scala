package uk.gov.homeoffice.drt.jsonformats

import org.scalatest.wordspec.AnyWordSpec
import spray.json.enrichAny
import uk.gov.homeoffice.drt.jsonformats.PassengersSummaryFormat.JsonFormat
import uk.gov.homeoffice.drt.model.PassengersSummary
import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.time.LocalDate

class PassengersSummaryFormatSpec extends AnyWordSpec {
  "PassengersSummaryFormat" should {
    "serialise and deserialise a PassengersSummary without loss" in {
      val passengersSummary = PassengersSummary(
        "regionName",
        "portCode",
        Some("terminalName"),
        2,
        1,
        Map(Queues.EeaDesk -> 1),
        Some(LocalDate(2020, 1, 1)),
        Some(1)
      )
      val serialised = passengersSummary.toJson
      val deserialised = serialised.convertTo[PassengersSummary]
      assert(deserialised == passengersSummary)
    }
  }
}
