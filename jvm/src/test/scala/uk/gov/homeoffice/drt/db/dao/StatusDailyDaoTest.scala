package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.tables.StatusDaily
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2}
import uk.gov.homeoffice.drt.time.LocalDate

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class StatusDailyDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  import TestDatabase.profile.api._

  before {
    Await.result(
      TestDatabase.run(DBIO.seq(
        StatusDailyDao.table.schema.dropIfExists,
        StatusDailyDao.table.schema.createIfNotExists)
      ), 2.second)
  }

  "StatusDailyQueries replaceHours" should {
    "insert records into an empty table" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val statusDaily = StatusDaily(portCode, terminal, LocalDate(2020, 1, 1), Option(1), Option(1), Option(1), Option(1))

      val update = StatusDailyDao.insertOrUpdate(portCode)
      Await.result(TestDatabase.run(update(statusDaily)), 2.second)

      val get = StatusDailyDao.get(portCode)
      val maybeStatusDaily = Await.result(TestDatabase.run(get(terminal, LocalDate(2020, 1, 1))), 1.second)

      maybeStatusDaily should be(Some(statusDaily))
    }

    "only insert/update entries for the port and terminal specified" in {
      val portCode = PortCode("LHR")
      val otherPortCode = PortCode("JFK")
      val terminal = T2
      val otherTerminal = T1
      val statusDailies = List(
        StatusDaily(otherPortCode, otherTerminal, LocalDate(2020, 1, 1), Option(1), Option(1), Option(1), Option(1)),
        StatusDaily(otherPortCode, otherTerminal, LocalDate(2020, 1, 1), Option(1), Option(1), Option(1), Option(1)),
        StatusDaily(portCode, terminal, LocalDate(2020, 1, 1), Option(3), Option(3), Option(3), Option(3)),
      )

      val insert = StatusDailyDao.insertOrUpdate(portCode)
      statusDailies.foreach { daily =>
        Await.result(TestDatabase.run(insert(daily)), 2.second)
      }
      val get = StatusDailyDao.get(portCode)
      val maybeStatus = Await.result(TestDatabase.run(get(terminal, LocalDate(2020, 1, 1))), 1.second)
      maybeStatus should be (Some(StatusDaily(portCode, terminal, LocalDate(2020, 1, 1), Option(3), Option(3), Option(3), Option(3))))
    }

    "update the updatedAt fields" in {
      val portCode = PortCode("LHR")
      val terminal = T2
      val statusDaily = StatusDaily(portCode, terminal, LocalDate(2020, 1, 1), Option(1), Option(1), Option(1), Option(1))

      val insert = StatusDailyDao.insertOrUpdate(portCode)
      Await.result(TestDatabase.run(insert(statusDaily)), 2.second)

      val update = StatusDailyDao.setUpdatedAt(portCode)(_.paxLoadsUpdatedAt)
      Await.result(TestDatabase.run(update(terminal, LocalDate(2020, 1, 1), 2)), 2.second)

      val get = StatusDailyDao.get(portCode)
      val maybeStatusDaily = Await.result(TestDatabase.run(get(terminal, LocalDate(2020, 1, 1))), 1.second)

      maybeStatusDaily should be(Some(statusDaily.copy(paxLoadsUpdatedAt = Option(2))))
    }
  }
}
