package uk.gov.homeoffice.drt.db.dao

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.tables.UserFeedbackRow

import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class UserFeedbackDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  import TestDatabase.profile.api._

  val userFeedbackDao: UserFeedbackDao = UserFeedbackDao(TestDatabase)

  before {
    Await.result(
      TestDatabase.run(DBIO.seq(
        userFeedbackDao.table.schema.dropIfExists,
        userFeedbackDao.table.schema.createIfNotExists)
      ), 2.second)
  }

  def getUserFeedBackRow(createdAt: Timestamp): UserFeedbackRow = {
    UserFeedbackRow(email = "test@test.com",
      createdAt = createdAt,
      bfRole = "test",
      drtQuality = "Good",
      drtLikes = Option("Arrivals"),
      drtImprovements = Option("Staffing"),
      participationInterest = true,
      feedbackType = Option("test"),
      abVersion = Option("A"))
  }

  "UserFeedbackDao" should {
    "should return a list of user feedback submitted" in {
      val userFeedbackRow = getUserFeedBackRow(new Timestamp(Instant.now().minusSeconds(60).toEpochMilli))

      Await.result(userFeedbackDao.insertOrUpdate(userFeedbackRow), 1.second)
      val userFeedbackResult = Await.result(userFeedbackDao.selectAll(), 1.second)

      userFeedbackResult should ===(Seq(userFeedbackRow))
    }

    "should return a list of user feedback using stream" in {
      implicit val system: ActorSystem = ActorSystem("testSystem")

      val userFeedbackRow1 = getUserFeedBackRow(new Timestamp(Instant.now().minusSeconds(60).toEpochMilli))
      val userFeedbackRow2 = getUserFeedBackRow(new Timestamp(Instant.now().minusSeconds(60).toEpochMilli)).copy(email = "test1@test.com")

      Await.result(userFeedbackDao.insertOrUpdate(userFeedbackRow1), 1.second)
      Await.result(userFeedbackDao.insertOrUpdate(userFeedbackRow2), 1.second)
      userFeedbackDao.selectAllAsStream().runWith(Sink.seq)
        .map(_ should ===(Seq(userFeedbackRow1, userFeedbackRow2)))
    }

    "should return a list of user feedback for given email" in {
      val userFeedbackRow = getUserFeedBackRow(new Timestamp(Instant.now().minusSeconds(60).toEpochMilli))
      val secondRow = userFeedbackRow.copy(email = "test2@test.com")
      Await.result(userFeedbackDao.insertOrUpdate(userFeedbackRow), 1.second)
      Await.result(userFeedbackDao.insertOrUpdate(secondRow), 1.second)
      userFeedbackDao.selectByEmail("test2@test.com")
        .map { userFeedbackResult =>
          userFeedbackResult should ===(Seq(secondRow))
        }
    }
  }
}
