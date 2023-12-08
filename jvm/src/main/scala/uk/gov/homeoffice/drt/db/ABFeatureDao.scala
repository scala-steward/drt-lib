package uk.gov.homeoffice.drt.db

import slick.lifted.{ProvenShape, TableQuery, Tag}
import uk.gov.homeoffice.drt.ABFeature
import slick.jdbc.PostgresProfile.api._

import java.sql.Timestamp
import scala.concurrent.Future

trait IABFeatureDao {
  def insertOrUpdate(aBFeatureRow: ABFeatureRow): Future[Int]

  def getABFeatures: Future[Seq[ABFeatureRow]]

  def getABFeaturesByEmailForFunction(email: String, functionName: String): Future[Seq[ABFeatureRow]]

  def getABFeatureByFunctionName(functionName: String): Future[Seq[ABFeatureRow]]
}

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

case class ABFeatureDao(db: Database) extends IABFeatureDao {
  val abFeatureTable: TableQuery[ABFeatureTable] = TableQuery[ABFeatureTable]

  override def insertOrUpdate(aBFeatureRow: ABFeatureRow): Future[Int] = {
    db.run(abFeatureTable insertOrUpdate aBFeatureRow)
  }

  override def getABFeatures: Future[Seq[ABFeatureRow]] = {
    db.run(abFeatureTable.result).mapTo[Seq[ABFeatureRow]]
  }

  override def getABFeatureByFunctionName(functionName: String): Future[Seq[ABFeatureRow]] = {
    db.run(abFeatureTable.filter(_.functionName === functionName).result).mapTo[Seq[ABFeatureRow]]
  }

  override def getABFeaturesByEmailForFunction(email: String, functionName: String): Future[Seq[ABFeatureRow]] =
    db.run(abFeatureTable.filter(ab => ab.email === email && ab.functionName === functionName).result).mapTo[Seq[ABFeatureRow]]

}
