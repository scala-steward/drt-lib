package uk.gov.homeoffice.drt.db.dao

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.QueueSlotSerialiser
import uk.gov.homeoffice.drt.db.tables.{QueueSlotRow, QueueSlotTable}
import uk.gov.homeoffice.drt.models.{CrunchMinute, TQM}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{DateRange, UtcDate}

import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}


case class QueueSlotDao()
                       (implicit ec: ExecutionContext) {
  val table: TableQuery[QueueSlotTable] = TableQuery[QueueSlotTable]

  def printlnCreateStatements(): Unit = {
    println(table.schema.createStatements.mkString(";\n") + ";")
  }

  def queueSlotsForDateRange(portCode: PortCode,
                             slotLengthMinutes: Int,
                             execute: DBIOAction[(UtcDate, Seq[CrunchMinute]), NoStream, Effect.Read] => Future[(UtcDate, Seq[CrunchMinute])]
                            ): (UtcDate, UtcDate, Seq[Terminal]) => Source[(UtcDate, Seq[CrunchMinute]), NotUsed] = {
    val getMinutes = getForTerminalsUtcDate(portCode, slotLengthMinutes)

    (start, end, terminals) =>
      Source(DateRange(start, end))
        .mapAsync(1) { date =>
          execute(getMinutes(terminals, date).map(date -> _))
        }
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

  def getForTerminalsUtcDate(port: PortCode, slotLengthMinutes: Int): (Seq[Terminal], UtcDate) => DBIOAction[Seq[CrunchMinute], NoStream, Effect.Read] =
    (terminals, date) =>
      table
        .filter(m =>
          m.port === port.iata &&
            m.terminal.inSet(terminals.map(_.toString)) &&
            m.slotDateUtc === date.toISOString &&
            m.slotLengthMinutes === slotLengthMinutes
        )
        .result
        .map(_.map(QueueSlotSerialiser.fromRow))

  def getForUtcDate(port: PortCode, slotLengthMinutes: Int): UtcDate => DBIOAction[Seq[CrunchMinute], NoStream, Effect.Read] =
    date =>
      table
        .filter(m =>
          m.port === port.iata &&
            m.slotDateUtc === date.toISOString &&
            m.slotLengthMinutes === slotLengthMinutes
        )
        .result
        .map(_.map(QueueSlotSerialiser.fromRow))

  def insertOrUpdate(portCode: PortCode, slotLengthMinutes: Int): CrunchMinute => DBIOAction[Int, NoStream, Effect.Write with Effect.Transactional] = {
    val toRow: (CrunchMinute, Int) => QueueSlotRow = QueueSlotSerialiser.toRow(portCode)

    crunchMinute =>
      table.insertOrUpdate(toRow(crunchMinute, slotLengthMinutes))
  }

  def updateAndRemoveSlots(portCode: PortCode, slotLengthMinutes: Int): (Iterable[CrunchMinute], Iterable[TQM]) => DBIOAction[Int, NoStream, Effect.Write with Nothing with Effect.Transactional] = {
    val insertOrUpdateSingle = insertOrUpdate(portCode, slotLengthMinutes)
    (updates, removals) => {
      val removalActions = removals.map { removal =>
        table
          .filter(row =>
            row.port === portCode.iata &&
              row.terminal === removal.terminal.toString &&
              row.queue === removal.queue.stringValue &&
              row.slotStart === new Timestamp(removal.minute) &&
              row.slotLengthMinutes === slotLengthMinutes
          )
          .delete
      }

      DBIO.sequence(removalActions).flatMap(_ => DBIO.sequence(updates.map(insertOrUpdateSingle))).map(_.size).transactionally
    }
  }

  def removeAllBefore(): UtcDate => DBIOAction[Int, NoStream, Effect.Write] = date =>
    table
      .filter(_.slotDateUtc < date.toISOString)
      .delete
}
