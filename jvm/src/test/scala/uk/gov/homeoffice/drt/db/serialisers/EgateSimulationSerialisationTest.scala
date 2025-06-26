package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.time.{SDate, UtcDate}

class EgateSimulationSerialisationTest extends AnyWordSpec {
  "EgateSimulationSerialisation" should {
    "serialize and deserialize EgateSimulationRow correctly" in {
      val simulation = EgateSimulation(
        uuid = "test-simulation",
        EgateSimulationRequest(
          startDate = UtcDate(2023, 10, 1),
          endDate = UtcDate(2023, 10, 2),
          terminal = T1,
          uptakePercentage = 75.0,
          parentChildRatio = 1.5,
        ),
        status = "active",
        createdAt = SDate("2023-10-01T12:00:00Z"),
      )

      val row = EgateSimulationSerialisation(simulation)
      val deserializedSimulation = EgateSimulationSerialisation(row)

      assert(deserializedSimulation == simulation)
    }
  }
}
