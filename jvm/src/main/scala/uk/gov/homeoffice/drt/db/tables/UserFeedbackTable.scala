package uk.gov.homeoffice.drt.db.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import uk.gov.homeoffice.drt.feedback.UserFeedback

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

  def * = (email, createdAt, feedbackType, bfRole, drtQuality, drtLikes, drtImprovements, participationInterest, abVersion) <> (UserFeedbackRow.tupled, UserFeedbackRow.unapply)
}

