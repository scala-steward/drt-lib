package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.serialisers.{EgateSimulation, EgateSimulationRequest, EgateSimulationResponse}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class EgateSimulationDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {

  import TestDatabase.profile.api._

  private val dao: EgateSimulationDao = EgateSimulationDao()

  SchemaUtils.printStatements(dao.table.schema.createStatements)

  before {
    Await.result(
      TestDatabase.run(DBIO.seq(
        dao.table.schema.dropIfExists,
        dao.table.schema.createIfNotExists)
      ), 2.second)
  }

  val date: SDateLike = SDate("2023-10-01T00:00:00Z")

  val simulation: EgateSimulation = EgateSimulation(
    uuid = "test-uuid",
    EgateSimulationRequest(
      portCode = PortCode("LHR"),
      terminal = T1,
      startDate = date.toUtcDate,
      endDate = date.toUtcDate,
      uptakePercentage = 50,
      parentChildRatio = 0.5),
    status = "active",
    response = Some(EgateSimulationResponse(
      csvContent = "header1,header2\nvalue1,value2",
      meanAbsolutePercentageError = 5.0,
      standardDeviation = 1.0,
      bias = 0.1,
      correlationCoefficient = 0.9,
      rSquaredError = 0.8,
    )),
    createdAt = date,
  )

  "insertOrUpdate" should {
    "insert a new egate simulation" in {
      Await.result(TestDatabase.run(dao.insertOrUpdate(simulation)), 2.second)

      val retrievedSimulation = Await.result(TestDatabase.run(dao.get(simulation.uuid)), 2.second)
      retrievedSimulation shouldBe Some(simulation)
    }

    "update an existing egate simulation" in {
      val initialSimulation = simulation

      Await.result(TestDatabase.run(dao.insertOrUpdate(initialSimulation)), 2.second)

      val updatedSimulation = initialSimulation.copy(status = "completed", response = Some(EgateSimulationResponse("nice csv content", 6.0, 1.5, 0.2, 0.95, 0.85)))
      Await.result(TestDatabase.run(dao.insertOrUpdate(updatedSimulation)), 2.second)

      val retrievedSimulation = Await.result(TestDatabase.run(dao.get(updatedSimulation.uuid)), 2.second)
      retrievedSimulation shouldBe Some(updatedSimulation)
    }
  }

  "get" should {
    "retrieve an egate simulation by UUID" in {
      Await.result(TestDatabase.run(dao.insertOrUpdate(simulation)), 2.second)

      val retrievedSimulation = Await.result(TestDatabase.run(dao.get(simulation.uuid)), 2.second)
      retrievedSimulation shouldBe Some(simulation)
    }

    "return None for a non-existent UUID" in {
      val retrievedSimulation = Await.result(TestDatabase.run(dao.get("non-existent-uuid")), 2.second)
      retrievedSimulation shouldBe None
    }
  }
}
