package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.QueueSlotSerialiser
import uk.gov.homeoffice.drt.db.tables.{QueueSlotRow, QueueSlotTable}
import uk.gov.homeoffice.drt.model.CrunchMinute
import uk.gov.homeoffice.drt.ports.PortCode

import java.sql.Timestamp
import scala.concurrent.ExecutionContext


case class QueueSlotDao(portCode: PortCode)
                       (implicit ec: ExecutionContext) {
  val table: TableQuery[QueueSlotTable] = TableQuery[QueueSlotTable]

  val toRow: (CrunchMinute, Int) => QueueSlotRow = QueueSlotSerialiser.toRow(portCode)

  def get(port: String, terminal: String, queue: String, startTime: Long, slotLengthMinutes: Int): DBIOAction[Seq[CrunchMinute], NoStream, Effect.Read] =
    table
      .filter(f =>
        f.port === port &&
          f.terminal === terminal &&
          f.queue === queue &&
          f.slotStart === new Timestamp(startTime) &&
          f.slotLengthMinutes === slotLengthMinutes
      )
      .result
      .map(_.map(QueueSlotSerialiser.fromRow))

  def insertOrUpdate(crunchMinute: CrunchMinute, slotLengthMinutes: Int): DBIOAction[Int, NoStream, Effect.Write with Effect.Transactional] =
    table.insertOrUpdate(toRow(crunchMinute, slotLengthMinutes))
}
