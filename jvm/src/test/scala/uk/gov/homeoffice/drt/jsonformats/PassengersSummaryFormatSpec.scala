package uk.gov.homeoffice.drt.jsonformats

import org.scalatest.wordspec.AnyWordSpec
import spray.json.enrichAny
import uk.gov.homeoffice.drt.jsonformats.PassengersSummaryFormat.JsonFormat
import uk.gov.homeoffice.drt.models.PassengersSummary
import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.time.LocalDate

class PassengersSummaryFormatSpec extends AnyWordSpec {
  "PassengersSummaryFormat" should {
    "serialise and deserialise a PassengersSummary without loss" in {
      val passengersSummary = PassengersSummary(
        "regionName",
        "portCode",
        Option("terminalName"),
        2,
        Map(Queues.EeaDesk -> 1),
        Map(Queues.EeaDesk -> 2),
        Option(LocalDate(2020, 1, 1)),
        Option(1),
      )
      val serialised = passengersSummary.toJson
      val deserialised = serialised.convertTo[PassengersSummary]
      assert(deserialised == passengersSummary)
    }
  }
}
