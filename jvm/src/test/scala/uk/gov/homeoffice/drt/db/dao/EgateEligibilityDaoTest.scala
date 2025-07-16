package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.serialisers.EgateEligibility
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.time.{SDate, UtcDate}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class EgateEligibilityDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  import uk.gov.homeoffice.drt.db.TestDatabase.profile.api._

  private val dao: EgateEligibilityDao = EgateEligibilityDao()

  SchemaUtils.printStatements(dao.table.schema.createStatements)

  before {
    Await.result(
      TestDatabase.run(DBIO.seq(
        dao.table.schema.dropIfExists,
        dao.table.schema.createIfNotExists)
      ), 2.second)
  }

  "insertOrUpdate" should {
    "insert a new egate eligibility" in {
      val eligibility = EgateEligibility(
        port = PortCode("LHR"),
        terminal = T1,
        dateUtc = UtcDate(2023, 10, 1),
        totalPassengers = 1000,
        egateEligiblePct = 800,
        egateUnderAgePct = 200,
        createdAt = SDate("2023-10-01T12:00:00Z")
      )

      Await.result(TestDatabase.run(dao.insertOrUpdate(eligibility)), 2.second)

      val retrievedEligibility = Await.result(TestDatabase.run(dao.get(eligibility.port, eligibility.terminal, eligibility.dateUtc)), 2.second)
      retrievedEligibility shouldBe Some(eligibility)
    }

    "update an existing egate eligibility" in {
      val eligibility = EgateEligibility(
        port = PortCode("LHR"),
        terminal = T1,
        dateUtc = UtcDate(2023, 10, 1),
        totalPassengers = 1000,
        egateEligiblePct = 800,
        egateUnderAgePct = 200,
        createdAt = SDate("2023-10-01T12:00:00Z")
      )

      Await.result(TestDatabase.run(dao.insertOrUpdate(eligibility)), 2.second)

      val updatedEligibility = eligibility.copy(totalPassengers = 1200)
      Await.result(TestDatabase.run(dao.insertOrUpdate(updatedEligibility)), 2.second)

      val retrievedEligibility = Await.result(TestDatabase.run(dao.get(updatedEligibility.port, updatedEligibility.terminal, updatedEligibility.dateUtc)), 2.second)
      retrievedEligibility shouldBe Some(updatedEligibility)
    }
  }

  "get" should {
    "retrieve an existing egate eligibility" in {
      val eligibility = EgateEligibility(
        port = PortCode("LHR"),
        terminal = T1,
        dateUtc = UtcDate(2023, 10, 1),
        totalPassengers = 1000,
        egateEligiblePct = 800,
        egateUnderAgePct = 200,
        createdAt = SDate("2023-10-01T12:00:00Z")
      )

      Await.result(TestDatabase.run(dao.insertOrUpdate(eligibility)), 2.second)

      val retrievedEligibility = Await.result(TestDatabase.run(dao.get(eligibility.port, eligibility.terminal, eligibility.dateUtc)), 2.second)
      retrievedEligibility shouldBe Some(eligibility)
    }

    "return None for a non-existing egate eligibility" in {
      val retrievedEligibility = Await.result(TestDatabase.run(dao.get(PortCode("LHR"), T1, UtcDate(2023, 10, 2))), 2.second)
      retrievedEligibility shouldBe None
    }
  }
}
