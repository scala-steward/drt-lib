package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import specs2.arguments.sequential
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.tables.PortTerminalConfig
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class PortTerminalConfigDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  sequential

  private val db = TestDatabase.db

  import TestDatabase.profile.api._

  before {
    Await.result(
      db.run(DBIO.seq(
        PortTerminalConfigDao.table.schema.dropIfExists,
        PortTerminalConfigDao.table.schema.createIfNotExists)
      ), 2.second)
  }

  "replaceHours" should {
    "insert records into an empty table" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val portTerminalConfig = PortTerminalConfig(portCode, terminal, Option(10), 1L)

      val update = PortTerminalConfigDao.insertOrUpdate(portCode)
      Await.result(db.run(update(portTerminalConfig)), 2.second)

      val get = PortTerminalConfigDao.get(portCode)
      val maybePortTerminalConfig = Await.result(db.run(get(terminal)), 1.second)

      maybePortTerminalConfig should be(Some(portTerminalConfig))
    }

    "only insert/update entries for the port and terminal specified" in {
      val portCode = PortCode("LHR")
      val otherPortCode = PortCode("JFK")
      val terminal = T2
      val otherTerminal = T1
      val statusDailies = List(
        PortTerminalConfig(otherPortCode, otherTerminal, Option(11), 1L),
        PortTerminalConfig(otherPortCode, otherTerminal, Option(11), 1L),
        PortTerminalConfig(portCode, terminal, Option(10), 1L),
      )

      val insert = PortTerminalConfigDao.insertOrUpdate(portCode)
      statusDailies.foreach { daily =>
        Await.result(db.run(insert(daily)), 2.second)
      }
      val get = PortTerminalConfigDao.get(portCode)
      val maybeStatus = Await.result(db.run(get(terminal)), 1.second)
      maybeStatus should be (Some(PortTerminalConfig(portCode, terminal, Option(10), 1L)))
    }
  }
}
