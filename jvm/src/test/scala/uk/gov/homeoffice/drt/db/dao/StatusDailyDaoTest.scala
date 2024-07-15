package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.{StatusDaily, TestDatabase}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2}
import uk.gov.homeoffice.drt.time.{LocalDate, UtcDate}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class StatusDailyDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  private val db = TestDatabase.db

  import TestDatabase.profile.api._

  before {
    Await.result(
      db.run(DBIO.seq(
        StatusDailyDao.table.schema.dropIfExists,
        StatusDailyDao.table.schema.createIfNotExists)
      ), 2.second)
  }

  "StatusDailyQueries replaceHours" should {
    "insert records into an empty table" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val statusDaily = StatusDaily(portCode, terminal, UtcDate(2020, 1, 1), 1, 1, 1)

      val update = StatusDailyDao.insertOrUpdate(portCode)
      Await.result(db.run(update(statusDaily)), 2.second)

      val get = StatusDailyDao.get(portCode)
      val maybeStatusDaily = Await.result(db.run(get(terminal, LocalDate(2020, 1, 1))), 1.second)

      maybeStatusDaily should be(Some(statusDaily))
    }

    "only insert/update entries for the port and terminal specified" in {
      val portCode = PortCode("LHR")
      val otherPortCode = PortCode("JFK")
      val terminal = T2
      val otherTerminal = T1
      val statusDailies = List(
        StatusDaily(otherPortCode, otherTerminal, UtcDate(2020, 1, 1), 1, 1, 1),
        StatusDaily(otherPortCode, otherTerminal, UtcDate(2020, 1, 1), 1, 1, 1),
        StatusDaily(portCode, terminal, UtcDate(2020, 1, 1), 3, 3, 3),
      )

      val insert = StatusDailyDao.insertOrUpdate(portCode)
      statusDailies.foreach { daily =>
        Await.result(db.run(insert(daily)), 2.second)
      }
      val get = StatusDailyDao.get(portCode)
      val maybeStatus = Await.result(db.run(get(terminal, LocalDate(2020, 1, 1))), 1.second)
      maybeStatus should be (Some(StatusDaily(portCode, terminal, UtcDate(2020, 1, 1), 3, 3, 3)))
    }
  }
}
