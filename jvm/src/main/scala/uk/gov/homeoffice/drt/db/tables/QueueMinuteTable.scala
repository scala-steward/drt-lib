package uk.gov.homeoffice.drt.db.tables

import slick.lifted.Tag
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._

import java.sql.Timestamp

case class QueueSlotRow(port: String,
                        terminal: String,
                        queue: String,
                        slotStart: Timestamp,
                        slotLengthMinutes: Int,
                        paxLoad: Double,
                        workLoad: Double,
                        deskRec: Int,
                        waitTime: Int,
                        paxInQueue: Option[Int],
                        deployedDesks: Option[Int],
                        deployedWait: Option[Int],
                        deployedPaxInQueue: Option[Int],
                        updatedAt: Timestamp,
                       )

class QueueSlotTable(tag: Tag)
  extends Table[QueueSlotRow](tag, "queue_slot") {

  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def queue: Rep[String] = column[String]("queue")

  def slotStart: Rep[Timestamp] = column[Timestamp]("slot_start")

  def slotLengthMinutes: Rep[Int] = column[Int]("slot_length_minutes")

  def paxLoad: Rep[Double] = column[Double]("pax_load")

  def workLoad: Rep[Double] = column[Double]("work_load")

  def deskRec: Rep[Int] = column[Int]("desk_rec")

  def waitTime: Rep[Int] = column[Int]("wait_time")

  def paxInQueue: Rep[Option[Int]] = column[Option[Int]]("pax_in_queue")

  def deployedDesks: Rep[Option[Int]] = column[Option[Int]]("deployed_desks")

  def deployedWait: Rep[Option[Int]] = column[Option[Int]]("deployed_wait")

  def deployedPaxInQueue: Rep[Option[Int]] = column[Option[Int]]("deployed_pax_in_queue")

  def updatedAt: Rep[Timestamp] = column[Timestamp]("updated_at")

  def pk = primaryKey("pk_flight", (port, terminal, queue, slotStart, slotLengthMinutes))

  def * = (
    port,
    terminal,
    queue,
    slotStart,
    slotLengthMinutes,
    paxLoad,
    workLoad,
    deskRec,
    waitTime,
    paxInQueue,
    deployedDesks,
    deployedWait,
    deployedPaxInQueue,
    updatedAt).mapTo[QueueSlotRow]
}
