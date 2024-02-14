package uk.gov.homeoffice.drt.db

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class UserFeedbackDaoSpec extends Specification with BeforeEach {
  sequential

  lazy val db = TestDatabase.db

  import TestDatabase.profile.api._

  override def before = {
    Await.result(
      db.run(DBIO.seq(
        TestDatabase.userFeedbackTable.schema.dropIfExists,
        TestDatabase.userFeedbackTable.schema.createIfNotExists)
      ), 2.second)
  }

  def getUserFeedBackRow(createdAt: Timestamp) = {
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
      val userFeedbackDao = UserFeedbackDao(TestDatabase.db)
      val userFeedbackRow = getUserFeedBackRow(new Timestamp(Instant.now().minusSeconds(60).toEpochMilli))

      Await.result(userFeedbackDao.insertOrUpdate(userFeedbackRow), 1.second)
      val userFeedbackResult = Await.result(userFeedbackDao.selectAll(), 1.second)

      userFeedbackResult.size === 1
      userFeedbackResult.head === userFeedbackRow
    }

    "should return a list of user feedback using stream" in {
      implicit val system: ActorSystem = ActorSystem("testSystem")

      val userFeedbackDao = UserFeedbackDao(TestDatabase.db)
      val userFeedbackResult = Await.result(userFeedbackDao.selectAll(), 1.second)
      userFeedbackResult.size === 1
      val exitingRow = userFeedbackResult.head
      val userFeedbackRow = getUserFeedBackRow(new Timestamp(Instant.now().minusSeconds(60).toEpochMilli)).copy(email = "test1@test.com")

      Await.result(userFeedbackDao.insertOrUpdate(userFeedbackRow), 1.second)
      userFeedbackDao.selectAllAsStream().runWith(Sink.seq)
        .map { userFeedbackResult =>
          userFeedbackResult.size === 2
          userFeedbackResult === Seq(exitingRow, userFeedbackRow)
        }
    }

    "should return a list of user feedback for given email" in {
      val userFeedbackDao = UserFeedbackDao(TestDatabase.db)
      val userFeedbackRow = getUserFeedBackRow(new Timestamp(Instant.now().minusSeconds(60).toEpochMilli))
      val secondRow = userFeedbackRow.copy(email = "test2@test.com")
      Await.result(userFeedbackDao.insertOrUpdate(userFeedbackRow), 1.second)
      Await.result(userFeedbackDao.insertOrUpdate(secondRow), 1.second)
      userFeedbackDao.selectByEmail("test2@test.com")
        .map { userFeedbackResult =>
          userFeedbackResult.size === 1
          userFeedbackResult.head === secondRow
        }
    }
  }
}
