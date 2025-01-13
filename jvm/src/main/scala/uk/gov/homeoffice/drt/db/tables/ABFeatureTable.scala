package uk.gov.homeoffice.drt.db.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Tag}
import uk.gov.homeoffice.drt.ABFeature

import java.sql.Timestamp


case class ABFeatureRow(email: String, functionName: String, presentedAt: Timestamp, abVersion: String) {
  def toABFeature = ABFeature(email, functionName, presentedAt.getTime, abVersion)
}

class ABFeatureTable(tag: Tag) extends Table[ABFeatureRow](tag, "ab_feature") {

  def email = column[String]("email")

  def functionName = column[String]("function_name")

  def presentedAt = column[java.sql.Timestamp]("presented_at")

  def abVersion = column[String]("ab_version")

  val pk = primaryKey("ab_feature_pkey", (email, functionName))

  def * : ProvenShape[ABFeatureRow] = (email, functionName, presentedAt, abVersion).mapTo[ABFeatureRow]
}
