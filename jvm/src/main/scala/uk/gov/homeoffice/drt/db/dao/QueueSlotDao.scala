package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.QueueSlotSerialiser
import uk.gov.homeoffice.drt.db.tables.{QueueSlotRow, QueueSlotTable}
import uk.gov.homeoffice.drt.model.CrunchMinute
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp
import scala.concurrent.ExecutionContext


case class QueueSlotDao()
                       (implicit ec: ExecutionContext) {
  val table: TableQuery[QueueSlotTable] = TableQuery[QueueSlotTable]

  def printlnCreateStatements(): Unit = {
    println(table.schema.createStatements.mkString(";\n") + ";")
  }

  def get(port: PortCode, slotLengthMinutes: Int): (Terminal, Queue, Long) => DBIOAction[Seq[CrunchMinute], NoStream, Effect.Read] =
    (terminal, queue, startTime) =>
      table
        .filter(f =>
          f.port === port.iata &&
            f.terminal === terminal.toString &&
            f.queue === queue.stringValue &&
            f.slotStart === new Timestamp(startTime) &&
            f.slotLengthMinutes === slotLengthMinutes
        )
        .result
        .map(_.map(QueueSlotSerialiser.fromRow))

  def getForDatePortTerminalDate(port: PortCode): (Terminal, UtcDate) => DBIOAction[Seq[CrunchMinute], NoStream, Effect.Read] =
    (terminal, date) =>
      table
        .filter(f =>
          f.port === port.iata &&
            f.terminal === terminal.toString &&
            f.slotDateUtc === date.toISOString
        )
        .result
        .map(_.map(QueueSlotSerialiser.fromRow))

  def getForDatePortDate(port: PortCode): UtcDate => DBIOAction[Seq[CrunchMinute], NoStream, Effect.Read] =
    date =>
      table
        .filter(f => f.port === port.iata && f.slotDateUtc === date.toISOString)
        .result
        .map(_.map(QueueSlotSerialiser.fromRow))

  def insertOrUpdate(portCode: PortCode, slotLengthMinutes: Int): CrunchMinute => DBIOAction[Int, NoStream, Effect.Write with Effect.Transactional] = {
    val toRow: (CrunchMinute, Int) => QueueSlotRow = QueueSlotSerialiser.toRow(portCode)

    crunchMinute =>
      table.insertOrUpdate(toRow(crunchMinute, slotLengthMinutes))
  }

  def insertOrUpdateMulti(portCode: PortCode, slotLengthMinutes: Int): Iterable[CrunchMinute] => DBIOAction[Int, NoStream, Effect.Write with Effect.Transactional] = {
    val insertOrUpdateSingle = insertOrUpdate(portCode, slotLengthMinutes)
    crunchMinutes =>
      DBIO.sequence(crunchMinutes.map(insertOrUpdateSingle)).map(_.sum)
  }

  def removeAllBefore: UtcDate => DBIOAction[Int, NoStream, Effect.Write] = date =>
    table
      .filter(_.slotDateUtc < date.toISOString)
      .delete
}
