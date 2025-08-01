package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.time.{SDate, UtcDate}

class EgateSimulationSerialisationTest extends AnyWordSpec {
  "EgateSimulationSerialisation" should {
    "serialize and deserialize EgateSimulationRow correctly" in {
      val simulation = EgateSimulation(
        uuid = "test-simulation",
        EgateSimulationRequest(
          portCode = PortCode("LHR"),
          terminal = T1,
          startDate = UtcDate(2023, 10, 1),
          endDate = UtcDate(2023, 10, 2),
          uptakePercentage = 75.0,
          parentChildRatio = 1.5,
        ),
        status = "active",
        response = Some(EgateSimulationResponse(
          "simulation content",
          meanAbsolutePercentageError = 10.0,
          standardDeviation = 2.0,
          bias = 0.5,
          correlationCoefficient = 0.95,
          rSquaredError = 0.9,
        )),
        createdAt = SDate("2023-10-01T12:00:00Z"),
      )

      val row = EgateSimulationSerialisation(simulation)
      val deserializedSimulation = EgateSimulationSerialisation(row)

      assert(deserializedSimulation == simulation)
    }
  }
}
