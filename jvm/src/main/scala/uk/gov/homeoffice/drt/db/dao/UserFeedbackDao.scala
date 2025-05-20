package uk.gov.homeoffice.drt.db.dao

import org.apache.pekko.stream.scaladsl.Source
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import uk.gov.homeoffice.drt.db.CentralDatabase
import uk.gov.homeoffice.drt.db.tables.{UserFeedbackRow, UserFeedbackTable}

import scala.concurrent.{ExecutionContext, Future}

trait IUserFeedbackDao {
  def insertOrUpdate(userFeedbackRow: UserFeedbackRow): Future[Int]

  def selectAllAsStream(): Source[UserFeedbackRow, _]

  def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserFeedbackRow]]

  def selectByEmail(email: String): Future[Seq[UserFeedbackRow]]

}

case class UserFeedbackDao(appDb: CentralDatabase) extends IUserFeedbackDao {
  val table: TableQuery[UserFeedbackTable] = TableQuery[UserFeedbackTable]

  override def insertOrUpdate(userFeedbackRow: UserFeedbackRow): Future[Int] = {
    appDb.db.run(table insertOrUpdate userFeedbackRow)
  }

  override def selectAllAsStream(): Source[UserFeedbackRow, _] = {
    Source.fromPublisher(appDb.db.stream(table.result))
  }

  override def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserFeedbackRow]] = {
    appDb.db.run(table.result).mapTo[Seq[UserFeedbackRow]]
  }

  override def selectByEmail(email: String): Future[Seq[UserFeedbackRow]] = appDb.db.run(table.filter(_.email === email).result)
}
