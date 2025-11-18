package uk.gov.homeoffice.drt.db.dao

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import uk.gov.homeoffice.drt.Shift
import uk.gov.homeoffice.drt.db.CentralDatabase
import uk.gov.homeoffice.drt.db.tables.{StaffShiftRow, StaffShiftsTable}
import uk.gov.homeoffice.drt.time.LocalDate
import uk.gov.homeoffice.drt.util.ShiftUtil.{convertToSqlDate, fromStaffShiftRow, toStaffShiftRow}

import java.sql.Date
import scala.concurrent.{ExecutionContext, Future}

trait IStaffShiftsDao {
  def insertOrUpdate(shift: Shift): Future[Int]

  def updateStaffShift(previousShift: Shift, futureExistingShift: Shift, shiftRow: Shift): Future[Shift]

  def updateStaffShift(previousShift: Shift, shiftRow: Shift): Future[Shift]

  def createNewShiftWhileEditing(previousShift: Shift, shiftRow: Shift): Future[Shift]

  def getStaffShiftsByPort(port: String): Future[Seq[Shift]]

  def getStaffShiftsByPortAndTerminal(port: String, terminal: String): Future[Seq[Shift]]

  def getStaffShift(port: String, terminal: String, shiftName: String, startDate: LocalDate, startTime: String): Future[Option[Shift]]

  def getOverlappingStaffShifts(port: String, terminal: String, shift: Shift): Future[Seq[Shift]]

  def searchStaffShift(port: String, terminal: String, shiftName: String, startDate: LocalDate): Future[Option[Shift]]

  def latestStaffShiftForADate(port: String, terminal: String, startDate: LocalDate, startTime: String): Future[Option[Shift]]

  def latestShiftAfterStartDateExists(newShift: Shift): Future[Option[Shift]]

  def deleteStaffShift(port: String, terminal: String, shiftName: String, shiftStartDate: LocalDate, startTime: String): Future[Option[Shift]]

  def deleteStaffShifts(): Future[Int]
}

case class StaffShiftsDao(db: CentralDatabase)(implicit ec: ExecutionContext) extends IStaffShiftsDao {
  val staffShiftsTable: TableQuery[StaffShiftsTable] = TableQuery[StaffShiftsTable]

  override def insertOrUpdate(shift: Shift): Future[Int] =
    db.run(staffShiftsTable.insertOrUpdate(toStaffShiftRow(shift.copy(createdAt = System.currentTimeMillis()))))

  def updateStaffShift(previousShift: Shift, shiftRow: Shift): Future[Shift] = {
    val previousStaffShiftRow: StaffShiftRow = toStaffShiftRow(previousShift)
    val staffShiftRow: StaffShiftRow = toStaffShiftRow(shiftRow)

    val deleteAction = staffShiftsTable
      .filter(row =>
        row.port === previousStaffShiftRow.port &&
          row.terminal === previousStaffShiftRow.terminal &&
          row.shiftName === previousStaffShiftRow.shiftName &&
          row.startDate === previousStaffShiftRow.startDate &&
          row.startTime === previousStaffShiftRow.startTime
      ).delete

    val updatesStaffShiftRow = if (previousStaffShiftRow.endDate.isDefined)
      staffShiftRow.copy(endDate = previousStaffShiftRow.endDate)
    else staffShiftRow.copy(endDate = None)
    val insertAction = staffShiftsTable += updatesStaffShiftRow
    db.run(deleteAction.andThen(insertAction)).map(_ => fromStaffShiftRow(updatesStaffShiftRow))
  }

  override def getStaffShiftsByPort(port: String): Future[Seq[Shift]] =
    db.run(staffShiftsTable.filter(_.port === port).sortBy(_.startDate.desc).result).map(_.map(fromStaffShiftRow))

  override def getStaffShiftsByPortAndTerminal(port: String, terminal: String): Future[Seq[Shift]] =
    db.run(staffShiftsTable.filter(s => s.port === port && s.terminal === terminal).sortBy(_.startDate.desc).result).map(_.map(fromStaffShiftRow))

  def deleteStaffShift(port: String, terminal: String, shiftName: String, shiftStartDate: LocalDate, startTime: String): Future[Option[Shift]] = {
    val startDate = convertToSqlDate(shiftStartDate)

    val shiftToBeDeletedOptFut = getStaffShift(port, terminal, shiftName, shiftStartDate, startTime)
    val deleteAction = staffShiftsTable
      .filter(row =>
        row.port === port &&
          row.terminal === terminal &&
          row.shiftName === shiftName &&
          row.startDate === startDate &&
          row.startTime === startTime
      ).delete

    db.run(deleteAction).flatMap(_ => shiftToBeDeletedOptFut)
  }

  override def deleteStaffShifts(): Future[Int] = db.run(staffShiftsTable.delete)

  override def getStaffShift(port: String, terminal: String, shiftName: String, startDate: LocalDate, startTime: String): Future[Option[Shift]] = {
    val startDateSql = Date.valueOf(startDate.toString)
    db.run(staffShiftsTable.filter(s => s.port === port &&
      s.terminal === terminal &&
      s.shiftName.toLowerCase === shiftName.toLowerCase &&
      s.startDate === startDateSql &&
      s.startTime === startTime
    ).sortBy(_.startDate.desc).result.headOption).map(_.map(fromStaffShiftRow))
  }

  override def searchStaffShift(port: String, terminal: String, shiftName: String, startDate: LocalDate): Future[Option[Shift]] = {
    val startDateSql = Date.valueOf(startDate.toString)
    db.run(staffShiftsTable.filter(s => s.port === port &&
      s.terminal === terminal &&
      s.shiftName.toLowerCase === shiftName.toLowerCase &&
      s.startDate === startDateSql
    ).sortBy(_.startDate.desc).result.headOption).map(_.map(fromStaffShiftRow))
  }

  override def getOverlappingStaffShifts(port: String, terminal: String, shift: Shift): Future[Seq[Shift]] = {
    val staffShiftRow = toStaffShiftRow(shift)
    db.run(
      staffShiftsTable.filter { s =>
        s.port === port &&
          s.terminal === terminal &&
          (s.endDate.isEmpty || s.endDate.map(_ > staffShiftRow.startDate).getOrElse(false))
      }.sortBy(_.startDate.desc).result
    ).map(_.map(fromStaffShiftRow))
  }

  override def latestStaffShiftForADate(port: String, terminal: String, date: LocalDate, startTime: String): Future[Option[Shift]] = {
    val startDateSql = Date.valueOf(date.toString)
    val (startTimeHours, startTimeMinutes) = startTime.split(":") match {
      case Array(hours, minutes) => (hours.toInt, minutes.toInt)
      case _ => throw new IllegalArgumentException(s"Invalid start time format: $startTime")
    }

    val lowerBoundStr = f"${startTimeHours - 3}%02d:$startTimeMinutes%02d"
    val upperBoundStr = f"${startTimeHours + 3}%02d:$startTimeMinutes%02d"

    db.run(staffShiftsTable.filter(s => s.port === port && s.terminal === terminal &&
      (s.startDate === startDateSql || s.startDate <= startDateSql && (s.endDate >= startDateSql || s.endDate.isEmpty)) &&
      s.startTime >= lowerBoundStr &&
      s.startTime < upperBoundStr
    ).sortBy(_.startDate.desc).result.headOption).map(_.map(fromStaffShiftRow))
  }

  override def createNewShiftWhileEditing(previousShift: Shift, newShift: Shift): Future[Shift] = {
    val previousStaffShiftRow: StaffShiftRow = toStaffShiftRow(previousShift)
    val staffShiftRow: StaffShiftRow = toStaffShiftRow(newShift)

    val deleteAction = staffShiftsTable
      .filter(row =>
        row.port === previousStaffShiftRow.port &&
          row.terminal === previousStaffShiftRow.terminal &&
          row.shiftName === previousStaffShiftRow.shiftName &&
          row.startDate === previousStaffShiftRow.startDate &&
          row.startTime === previousStaffShiftRow.startTime
      ).delete

    val endDateADayBefore = new java.sql.Date(staffShiftRow.startDate.getTime - 24L * 60 * 60 * 1000)
    val updatesStaffShiftRow = previousStaffShiftRow.copy(endDate = Option(endDateADayBefore))
    val insertUpdateAction = staffShiftsTable += updatesStaffShiftRow

    val insertNewAction = staffShiftsTable += staffShiftRow
    db.run(deleteAction.andThen(insertUpdateAction).andThen(insertNewAction)).map(_ => fromStaffShiftRow(staffShiftRow))
  }

  override def latestShiftAfterStartDateExists(newShift: Shift): Future[Option[Shift]] = {
    val newShiftRow = toStaffShiftRow(newShift)
    val (startTimeHours, startTimeMinutes) = newShift.startTime.split(":") match {
      case Array(hours, minutes) => (hours.toInt, minutes.toInt)
      case _ => throw new IllegalArgumentException(s"Invalid start time format: $newShift.startTime")
    }
    val lowerBoundStr = f"${startTimeHours - 3}%02d:$startTimeMinutes%02d"
    val upperBoundStr = f"${startTimeHours + 3}%02d:$startTimeMinutes%02d"

    db.run(
      staffShiftsTable.filter { s =>
        s.port === newShiftRow.port &&
          s.terminal === newShiftRow.terminal &&
          s.startDate >= newShiftRow.startDate &&
          s.startTime >= lowerBoundStr &&
          s.startTime < upperBoundStr
      }.sortBy(_.startDate.desc).result.headOption
    ).map(_.map(fromStaffShiftRow))
  }


  override def updateStaffShift(previousShift: Shift, futureExistingShift: Shift, shiftRow: Shift): Future[Shift] = {
    val previousStaffShiftRow: StaffShiftRow = toStaffShiftRow(previousShift)
    val futureExistingShiftRow: StaffShiftRow = toStaffShiftRow(futureExistingShift)
    val staffShiftRow: StaffShiftRow = toStaffShiftRow(shiftRow)

    val deletePreviousExistingAction = staffShiftsTable
      .filter(row =>
        row.port === previousStaffShiftRow.port &&
          row.terminal === previousStaffShiftRow.terminal &&
          row.shiftName === previousStaffShiftRow.shiftName &&
          row.startDate === previousStaffShiftRow.startDate &&
          row.startTime === previousStaffShiftRow.startTime
      ).delete

    val endDateADayBefore = new java.sql.Date(staffShiftRow.startDate.getTime - 24L * 60 * 60 * 1000)
    val updatePreviousExitingStaffShiftRow = if (previousStaffShiftRow.endDate.isDefined) previousStaffShiftRow.copy(endDate = Option(endDateADayBefore)) else previousStaffShiftRow.copy(endDate = None)
    val insertPreviousExitingAction = staffShiftsTable += updatePreviousExitingStaffShiftRow


    val updateFutureExitingStaffShiftRow = if (futureExistingShiftRow.endDate.isDefined) staffShiftRow.copy(endDate = futureExistingShiftRow.endDate) else staffShiftRow.copy(endDate = None)
    val insertFutureExitingAction = staffShiftsTable += updateFutureExitingStaffShiftRow


    val deleteFutureExistingAction = staffShiftsTable
      .filter(row =>
        row.port === futureExistingShiftRow.port &&
          row.terminal === futureExistingShiftRow.terminal &&
          row.shiftName === futureExistingShiftRow.shiftName &&
          row.startDate === futureExistingShiftRow.startDate &&
          row.startTime === futureExistingShiftRow.startTime
      ).delete

    val actions = for {
      _ <- deletePreviousExistingAction
      _ <- deleteFutureExistingAction
      _ <- insertPreviousExitingAction
      _ <- insertFutureExitingAction
    } yield ()
    db.run(actions.transactionally).map(_ => fromStaffShiftRow(updateFutureExitingStaffShiftRow))
  }
}
