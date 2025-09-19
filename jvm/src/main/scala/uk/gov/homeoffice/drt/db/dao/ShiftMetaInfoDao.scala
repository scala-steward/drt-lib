package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import slick.lifted.TableQuery
import uk.gov.homeoffice.drt.ShiftMeta
import uk.gov.homeoffice.drt.db.CentralDatabase
import uk.gov.homeoffice.drt.db.tables.{ShiftMetaInfoRow, ShiftMetaInfoTable}
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlAction

import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}

trait IShiftMetaInfoDaoLike {

  def insertShiftMetaInfo(shiftMeta: ShiftMeta)(implicit ex: ExecutionContext): Future[Int]

  def getShiftMetaInfo(port: String, terminal: String)(implicit ex: ExecutionContext): Future[Option[ShiftMeta]]

  def updateShiftAssignmentsMigratedAt(port: String, terminal: String, shiftAssignmentsMigratedAt: Option[Long])(implicit ex: ExecutionContext): Future[Option[ShiftMeta]]

}

case class ShiftMetaInfoDao(db: CentralDatabase) extends IShiftMetaInfoDaoLike {
  val shiftMetaInfoTable: TableQuery[ShiftMetaInfoTable] = TableQuery[ShiftMetaInfoTable]

  def insertShiftMetaInfo(shiftMeta: ShiftMeta)(implicit ex: ExecutionContext): Future[Int] = {
    val query = shiftMetaInfoTable += ShiftMetaInfoRow(shiftMeta.port, shiftMeta.terminal, shiftMeta.shiftAssignmentsMigratedAt.map(new java.sql.Timestamp(_)))
    db.db.run(query)
  }

  override def getShiftMetaInfo(port: String, terminal: String)(implicit ex: ExecutionContext): Future[Option[ShiftMeta]] = {
    val query = shiftMetaInfoTable.filter(row => row.port === port && row.terminal === terminal)
    val action = query.result.headOption
    shiftMetaData(action)
  }

  private def shiftMetaData(action: SqlAction[Option[ShiftMetaInfoRow], NoStream, Effect.Read])(implicit ex: ExecutionContext) = {
    db.db.run(action).map {
      case Some(row) => Some(ShiftMeta(row.port, row.terminal, row.shiftAssignmentsMigratedAtLong))
      case None => None
    }
  }

  override def updateShiftAssignmentsMigratedAt(port: String, terminal: String, shiftAssignmentsMigratedAt: Option[Long])(implicit ex: ExecutionContext): Future[Option[ShiftMeta]] = {
    val query = shiftMetaInfoTable.filter(row => row.port === port && row.terminal === terminal)
      .map(_.shiftAssignmentsMigratedAt)
      .update(shiftAssignmentsMigratedAt.map(new Timestamp(_)))
    val action = query.andThen(shiftMetaInfoTable.filter(row => row.port === port && row.terminal === terminal).result.headOption)
    actionShiftMetaData(action)
  }


  private def actionShiftMetaData(action: DBIOAction[Option[ShiftMetaInfoRow], NoStream, Effect.Write with Effect.Read])(implicit ex: ExecutionContext) = {
    db.db.run(action).map {
      case Some(row) => Some(ShiftMeta(row.port, row.terminal, row.shiftAssignmentsMigratedAtLong))
      case None => None
    }
  }

}