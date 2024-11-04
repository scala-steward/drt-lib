package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.tables.QueueSlotRow
import uk.gov.homeoffice.drt.model.CrunchMinute
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal

import java.sql.Timestamp

object QueueSlotSerialiser {
  val toRow: PortCode => (CrunchMinute, Int) => QueueSlotRow =
    portCode =>
      (cm, slotLengthMinutes) =>
        QueueSlotRow(
          port = portCode.iata,
          terminal = cm.terminal.toString,
          queue = cm.queue.stringValue,
          slotStart = new Timestamp(cm.minute),
          slotLengthMinutes = slotLengthMinutes,
          paxLoad = cm.paxLoad,
          workLoad = cm.workLoad,
          deskRec = cm.deskRec,
          waitTime = cm.waitTime,
          paxInQueue = cm.maybePaxInQueue,
          deployedDesks = cm.deployedDesks,
          deployedWait = cm.deployedWait,
          deployedPaxInQueue = cm.maybeDeployedPaxInQueue,
          updatedAt = new Timestamp(cm.lastUpdated.getOrElse(cm.minute)),
        )


  val fromRow: QueueSlotRow => CrunchMinute = {
    case row: QueueSlotRow =>
      CrunchMinute(
        terminal = Terminal(row.terminal),
        queue = Queue(row.queue),
        minute = row.slotStart.getTime,
        paxLoad = row.paxLoad,
        workLoad = row.workLoad,
        deskRec = row.deskRec,
        waitTime = row.waitTime,
        maybePaxInQueue = row.paxInQueue,
        deployedDesks = row.deployedDesks,
        deployedWait = row.deployedWait,
        maybeDeployedPaxInQueue = row.deployedPaxInQueue,
        lastUpdated = Option(row.updatedAt.getTime),
      )
  }
}
