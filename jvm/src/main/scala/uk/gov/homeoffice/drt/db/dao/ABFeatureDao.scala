package uk.gov.homeoffice.drt.db.dao

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import uk.gov.homeoffice.drt.db.tables.{ABFeatureRow, ABFeatureTable}

import scala.concurrent.Future


trait IABFeatureDao {
  def insertOrUpdate(aBFeatureRow: ABFeatureRow): Future[Int]

  def getABFeatures: Future[Seq[ABFeatureRow]]

  def getABFeaturesByEmailForFunction(email: String, functionName: String): Future[Seq[ABFeatureRow]]

  def getABFeatureByFunctionName(functionName: String): Future[Seq[ABFeatureRow]]
}

case class ABFeatureDao(db: Database) extends IABFeatureDao {
  val abFeatureTable: TableQuery[ABFeatureTable] = TableQuery[ABFeatureTable]

  override def insertOrUpdate(aBFeatureRow: ABFeatureRow): Future[Int] = {
    db.run(abFeatureTable.insertOrUpdate(aBFeatureRow))
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
