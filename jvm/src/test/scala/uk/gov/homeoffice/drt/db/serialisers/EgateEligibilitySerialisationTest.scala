package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.time.{SDate, UtcDate}

class EgateEligibilitySerialisationTest extends AnyWordSpec {
  "EgateEligibilitySerialisation" should {
    "serialize and deserialize EgateEligibilityRow correctly" in {
      val eligibility = EgateEligibility(
        port = PortCode("LHR"),
        terminal = T1,
        dateUtc = UtcDate(2023, 10, 1),
        totalPassengers = 1000,
        egateEligiblePct = 800,
        egateUnderAgePct = 200,
        createdAt = SDate("2023-10-01T12:00:00Z")
      )

      val row = EgateEligibilitySerialisation(eligibility)
      val deserializedEligibility = EgateEligibilitySerialisation(row)

      assert(deserializedEligibility == eligibility)
    }
  }
}
