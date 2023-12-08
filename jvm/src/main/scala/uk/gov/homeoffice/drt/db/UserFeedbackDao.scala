package uk.gov.homeoffice.drt.db

import akka.stream.scaladsl.Source
import slick.lifted.{ProvenShape, TableQuery, Tag}
import slick.jdbc.PostgresProfile.api._
import uk.gov.homeoffice.drt.feedback.UserFeedback

import scala.concurrent.{ExecutionContext, Future}

case class UserFeedbackRow(email: String,
                           createdAt: java.sql.Timestamp,
                           feedbackType: Option[String],
                           bfRole: String,
                           drtQuality: String,
                           drtLikes: Option[String],
                           drtImprovements: Option[String],
                           participationInterest: Boolean,
                           abVersion: Option[String]) {
  def toUserFeedback = UserFeedback(email, createdAt.getTime, feedbackType, bfRole, drtQuality, drtLikes, drtImprovements, participationInterest, abVersion)
}


class UserFeedbackTable(tag: Tag) extends Table[UserFeedbackRow](tag, "user_feedback") {

  def email = column[String]("email")

  def createdAt = column[java.sql.Timestamp]("created_at")

  def feedbackType = column[Option[String]]("feedback_type")

  def bfRole = column[String]("bf_role")

  def drtQuality = column[String]("drt_quality")

  def drtLikes = column[Option[String]]("drt_likes")

  def drtImprovements = column[Option[String]]("drt_improvements")

  def participationInterest = column[Boolean]("participation_interest")

  def abVersion = column[Option[String]]("ab_version")

  val pk = primaryKey("user_feedback_pkey", (email, createdAt))

  def * : ProvenShape[UserFeedbackRow] = (email, createdAt, feedbackType, bfRole, drtQuality, drtLikes, drtImprovements, participationInterest, abVersion).mapTo[UserFeedbackRow]
}

trait IUserFeedbackDao {
  def insertOrUpdate(userFeedbackRow: UserFeedbackRow): Future[Int]

  def selectAllAsStream(): Source[UserFeedbackRow, _]

  def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserFeedbackRow]]

  def selectByEmail(email: String): Future[Seq[UserFeedbackRow]]

}

case class UserFeedbackDao(db: Database) extends IUserFeedbackDao {
  val userFeedbackTable: TableQuery[UserFeedbackTable] = TableQuery[UserFeedbackTable]

  override def insertOrUpdate(userFeedbackRow: UserFeedbackRow): Future[Int] = {
    db.run(userFeedbackTable insertOrUpdate userFeedbackRow)
  }

  override def selectAllAsStream(): Source[UserFeedbackRow, _] = {
    Source.fromPublisher(db.stream(userFeedbackTable.result))
  }

  override def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserFeedbackRow]] = {
    db.run(userFeedbackTable.result).mapTo[Seq[UserFeedbackRow]]
  }

  override def selectByEmail(email: String): Future[Seq[UserFeedbackRow]] = db.run(userFeedbackTable.filter(_.email === email).result)
}
