package uk.gov.homeoffice.drt.db.dao

import akka.stream.scaladsl.Source
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import uk.gov.homeoffice.drt.db.tables.{UserFeedbackRow, UserFeedbackTable}

import scala.concurrent.{ExecutionContext, Future}

trait IUserFeedbackDao {
  def insertOrUpdate(userFeedbackRow: UserFeedbackRow): Future[Int]

  def selectAllAsStream(): Source[UserFeedbackRow, _]

  def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserFeedbackRow]]

  def selectByEmail(email: String): Future[Seq[UserFeedbackRow]]

}

case class UserFeedbackDao(db: Database) extends IUserFeedbackDao {
  val table: TableQuery[UserFeedbackTable] = TableQuery[UserFeedbackTable]

  override def insertOrUpdate(userFeedbackRow: UserFeedbackRow): Future[Int] = {
    db.run(table insertOrUpdate userFeedbackRow)
  }

  override def selectAllAsStream(): Source[UserFeedbackRow, _] = {
    Source.fromPublisher(db.stream(table.result))
  }

  override def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserFeedbackRow]] = {
    db.run(table.result).mapTo[Seq[UserFeedbackRow]]
  }

  override def selectByEmail(email: String): Future[Seq[UserFeedbackRow]] = db.run(table.filter(_.email === email).result)
}