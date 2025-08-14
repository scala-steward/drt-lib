package uk.gov.homeoffice.drt.util

import uk.gov.homeoffice.drt.Shift
import uk.gov.homeoffice.drt.db.tables.StaffShiftRow
import uk.gov.homeoffice.drt.time.LocalDate

import java.sql.Date
import java.time.{LocalDate => JavaLocalDate}

object ShiftUtil {
  def toStaffShiftRow(shift: Shift): StaffShiftRow = {
    StaffShiftRow(
      port = shift.port,
      terminal = shift.terminal,
      shiftName = shift.shiftName,
      startDate = convertToSqlDate(shift.startDate),
      startTime = shift.startTime,
      endTime = shift.endTime,
      endDate = shift.endDate.map(convertToSqlDate),
      staffNumber = shift.staffNumber,
      frequency = shift.frequency,
      createdBy = shift.createdBy,
      createdAt = new java.sql.Timestamp(shift.createdAt)
    )
  }

  def fromStaffShiftRow(row: StaffShiftRow): Shift = {
    Shift(
      port = row.port,
      terminal = row.terminal,
      shiftName = row.shiftName,
      startDate = convertToLocalDate(row.startDate),
      startTime = row.startTime,
      endTime = row.endTime,
      endDate = row.endDate.map(convertToLocalDate),
      staffNumber = row.staffNumber,
      frequency = row.frequency,
      createdBy = row.createdBy,
      createdAt = row.createdAt.getTime
    )
  }

  def convertToSqlDate(localDate: LocalDate): java.sql.Date = {
    val javaLocalDate = JavaLocalDate.of(localDate.year, localDate.month, localDate.day)
    Date.valueOf(javaLocalDate)
  }

  private def convertToLocalDate(sqlDate: java.sql.Date): LocalDate = {
    val localDate = sqlDate.toLocalDate
    LocalDate(localDate.getYear, localDate.getMonthValue, localDate.getDayOfMonth)
  }

  def currentLocalDate: LocalDate = {
    val now = JavaLocalDate.now()
    LocalDate(now.getYear, now.getMonthValue, now.getDayOfMonth)
  }

  def localDateFromString(date: String): LocalDate = {
    val parts = date.split("-")
    if (parts.length != 3) throw new IllegalArgumentException(s"Invalid date format: $date")
    LocalDate(parts(0).toInt, parts(1).toInt, parts(2).toInt)
  }

  def localDateFromString(date: Option[String]): LocalDate = {
    date.map(localDateFromString).getOrElse(currentLocalDate)
  }

  def localDateAddMonth(localDate: LocalDate, months: Int): LocalDate = {
    val javaLocalDate = JavaLocalDate.of(localDate.year, localDate.month, localDate.day)
    val newJavaLocalDate = javaLocalDate.plusMonths(months)
    LocalDate(newJavaLocalDate.getYear, newJavaLocalDate.getMonthValue, newJavaLocalDate.getDayOfMonth)
  }

  def localDateAddDays(localDate: LocalDate, days: Int): LocalDate = {
    val javaLocalDate = JavaLocalDate.of(localDate.year, localDate.month, localDate.day)
    val newJavaLocalDate = javaLocalDate.plusDays(days)
    LocalDate(newJavaLocalDate.getYear, newJavaLocalDate.getMonthValue, newJavaLocalDate.getDayOfMonth)
  }

}
