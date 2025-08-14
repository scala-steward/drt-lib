package uk.gov.homeoffice.drt.db.dao

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import slick.dbio.DBIO
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.time.LocalDate

import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import TestDatabase.profile.api._
import uk.gov.homeoffice.drt.Shift
import uk.gov.homeoffice.drt.util.ShiftUtil.{localDateAddDays, localDateAddMonth}

import scala.concurrent.ExecutionContext.Implicits.global

class StaffShiftsDaoSpec extends Specification with BeforeEach {
  sequential

  val dao: StaffShiftsDao = StaffShiftsDao(TestDatabase)

  override def before: Unit = {
    Await.result(
      TestDatabase.run(DBIO.seq(
        dao.staffShiftsTable.schema.dropIfExists,
        dao.staffShiftsTable.schema.createIfNotExists)
      ), 2.second)
  }

  def getStaffShiftRow: Shift = {
    Shift(
      port = "LHR",
      terminal = "T5",
      shiftName = "Morning",
      startDate = LocalDate(2021, 1, 1),
      startTime = "08:00",
      endTime = "16:00",
      endDate = None,
      staffNumber = 10,
      createdBy = Some("test@drt.com"),
      frequency = Some("Daily"),
      createdAt = Instant.now().toEpochMilli
    )
  }

  "StaffShiftsDao" should {
    "insert or update a staff shift" in {
      val staffShiftsDao = StaffShiftsDao(TestDatabase)
      val staffShiftRow = getStaffShiftRow

      val insertResult = Await.result(staffShiftsDao.insertOrUpdate(staffShiftRow), 1.second)
      insertResult === 1

      val selectResult: Seq[Shift] = Await.result(staffShiftsDao.getStaffShiftsByPortAndTerminal("LHR", "T5"), 1.second)

      val expectedResultShift = selectResult.map(s => staffShiftRow.copy(createdAt = s.createdAt))
      selectResult === expectedResultShift
    }

    "retrieve staff shifts by port" in {
      val staffShiftsDao = StaffShiftsDao(TestDatabase)
      val staffShiftRow = getStaffShiftRow

      Await.result(staffShiftsDao.insertOrUpdate(staffShiftRow), 1.second)

      val selectResult = Await.result(staffShiftsDao.getStaffShiftsByPort("LHR"), 1.second)

      val expectedResultShift = selectResult.map(s => staffShiftRow.copy(createdAt = s.createdAt))

      selectResult.size === 1
      selectResult.head === expectedResultShift.head
    }

    "delete a staff shift" in {
      val staffShiftsDao = StaffShiftsDao(TestDatabase)
      val staffShiftRow = getStaffShiftRow

      Await.result(staffShiftsDao.insertOrUpdate(staffShiftRow), 1.second)

      val deleteResult = Await.result(staffShiftsDao.deleteStaffShift("LHR", "T5", "Morning"), 1.second)
      deleteResult === 1

      val selectResult = Await.result(staffShiftsDao.getStaffShiftsByPortAndTerminal("LHR", "T5"), 1.second)
      selectResult.isEmpty === true
    }

    "searchStaffShift should not return the shift if shiftName not matches or start date not matches" in {
      val port = "LHR"
      val terminal = "T5"
      val currentDate = LocalDate(2025, 7, 1)
      val staffShift = getStaffShiftRow

      Await.result(dao.insertOrUpdate(staffShift), 1.second)
      val result = Await.result(
        dao.searchStaffShift(port, terminal, staffShift.shiftName, staffShift.startDate),
        1.second
      )
      result.isDefined === true
      val retrievedShift = result.get
      retrievedShift.port === staffShift.port
      retrievedShift.terminal === staffShift.terminal
      retrievedShift.shiftName === staffShift.shiftName
      retrievedShift.startDate.toString === staffShift.startDate.toString
      retrievedShift.startTime === staffShift.startTime
      retrievedShift.endTime === staffShift.endTime
      retrievedShift.endDate === staffShift.endDate
      retrievedShift.staffNumber === staffShift.staffNumber
      retrievedShift.createdBy === staffShift.createdBy
      retrievedShift.frequency === staffShift.frequency

      val resultWithoutShiftAsNameNotMatch = Await.result(
        dao.searchStaffShift(port, terminal, "Early", staffShift.startDate),
        1.second
      )

      resultWithoutShiftAsNameNotMatch.isDefined === false

      val resultWithoutShiftAsStartDateNotMatch = Await.result(
        dao.searchStaffShift(port, terminal, staffShift.shiftName, currentDate),
        1.second
      )

      resultWithoutShiftAsStartDateNotMatch.isDefined === false

    }

    "latestStaffShiftForADate should return the correct shift for startDate 01-07-2025" in {
      val port = "LHR"
      val terminal = "T5"
      val shiftName = "Early"
      val currentDate = LocalDate(2025, 7, 1)
      val staffShift = getStaffShiftRow.copy(port = port, terminal = terminal, shiftName = shiftName, startDate = LocalDate(2021, 7, 1))

      Await.result(dao.insertOrUpdate(staffShift), 1.second)
      val result = Await.result(
        dao.latestStaffShiftForADate(port, terminal, currentDate, staffShift.startTime),
        1.second
      )
      result.isDefined === true
      val retrievedShift = result.get
      retrievedShift.port === staffShift.port
      retrievedShift.terminal === staffShift.terminal
      retrievedShift.shiftName === staffShift.shiftName
      retrievedShift.startDate.toString === staffShift.startDate.toString
      retrievedShift.startTime === staffShift.startTime
      retrievedShift.endTime === staffShift.endTime
      retrievedShift.endDate === staffShift.endDate
      retrievedShift.staffNumber === staffShift.staffNumber
      retrievedShift.createdBy === staffShift.createdBy
      retrievedShift.frequency === staffShift.frequency
    }

    "searchStaffShift should return the correct shift" in {
      val staffShiftRow = getStaffShiftRow
      Await.result(dao.insertOrUpdate(staffShiftRow), 1.second)
      val result = Await.result(
        dao.searchStaffShift(
          staffShiftRow.port,
          staffShiftRow.terminal,
          staffShiftRow.shiftName,
          staffShiftRow.startDate
        ),
        1.second
      )

      val resultRow: Shift = result.get
      resultRow === staffShiftRow.copy(createdAt = resultRow.createdAt)
    }

    "getOverlappingStaffShifts should return overlapping shifts for searchShift" in {
      val baseShift, searchShift = getStaffShiftRow
      val overlappingShift = baseShift.copy(
        shiftName = "Overlapping",
        startDate = LocalDate(2020, 12, 31),
        startTime = "09:00",
        endTime = "17:00"
      )
      Await.result(dao.insertOrUpdate(baseShift), 1.second)
      Await.result(dao.insertOrUpdate(overlappingShift), 1.second)
      val result = Await.result(
        dao.getOverlappingStaffShifts(searchShift.port, searchShift.terminal, searchShift),
        1.second
      )
      result.map(_.startTime) must contain(allOf(baseShift.startTime, overlappingShift.startTime))

      result.size === 2

    }

    "getOverlappingStaffShifts should not return shifts where start date is later than search shift and end Date empty or one month or later" in {
      val baseShift, searchShift = getStaffShiftRow
      val oneMonthLater = localDateAddMonth(baseShift.startDate, 1)
      val overlappingShift = baseShift.copy(
        shiftName = "MonthLater",
        startDate = oneMonthLater,
        startTime = "09:00",
        endTime = "17:00"
      )
      Await.result(dao.insertOrUpdate(baseShift), 1.second)
      Await.result(dao.insertOrUpdate(overlappingShift), 1.second)
      val result = Await.result(
        dao.getOverlappingStaffShifts(searchShift.port, searchShift.terminal, searchShift),
        1.second
      )
      result.map(_.shiftName) must contain(baseShift.shiftName)
      result.size === 1
    }

    "getOverlappingStaffShifts should not return shifts where start date is before search shift and end Date previous one month" in {
      val baseShift, searchShift = getStaffShiftRow
      val oneMonthEarlier = localDateAddMonth(baseShift.startDate, -1)
      val overlappingShift = baseShift.copy(
        shiftName = "MonthEarly",
        endDate = Option(oneMonthEarlier),
        startTime = "09:00",
        endTime = "17:00"
      )
      Await.result(dao.insertOrUpdate(baseShift), 1.second)
      Await.result(dao.insertOrUpdate(overlappingShift), 1.second)
      val result = Await.result(
        dao.getOverlappingStaffShifts(searchShift.port, searchShift.terminal, searchShift),
        1.second
      )
      result.map(_.shiftName) must contain(baseShift.shiftName)
      result.size === 1
    }

    "getOverlappingStaffShifts should not return shift where endDate equals searchShift.startDate" in {
      val searchShift = getStaffShiftRow
      val edgeShift = searchShift.copy(
        shiftName = "EdgeCase",
        startDate = LocalDate(2020, 12, 31),
        endDate = Some(searchShift.startDate)
      )
      Await.result(dao.insertOrUpdate(edgeShift), 1.second)
      val result = Await.result(
        dao.getOverlappingStaffShifts(searchShift.port, searchShift.terminal, searchShift),
        1.second
      )
      result.map(_.shiftName) must not contain "EdgeCase"
    }

    "getOverlappingStaffShifts should not return shift where endDate is before startDate" in {
      val searchShift = getStaffShiftRow
      val invalidShift = searchShift.copy(
        shiftName = "Invalid",
        startDate = LocalDate(2020, 12, 31),
        endDate = Some(LocalDate(2020, 12, 30))
      )
      Await.result(dao.insertOrUpdate(invalidShift), 1.second)
      val result = Await.result(
        dao.getOverlappingStaffShifts(searchShift.port, searchShift.terminal, searchShift),
        1.second
      )
      result.map(_.shiftName) must not contain "Invalid"
    }

    "getOverlappingStaffShifts should return multiple overlapping shifts" in {
      val searchShift = getStaffShiftRow
      val overlap1 = searchShift.copy(
        shiftName = "Overlap1",
        startDate = LocalDate(2020, 12, 30),
        endDate = None
      )
      val overlap2 = searchShift.copy(
        shiftName = "Overlap2",
        startDate = LocalDate(2020, 12, 29),
        endDate = None
      )
      Await.result(dao.insertOrUpdate(overlap1), 1.second)
      Await.result(dao.insertOrUpdate(overlap2), 1.second)
      val result = Await.result(
        dao.getOverlappingStaffShifts(searchShift.port, searchShift.terminal, searchShift),
        1.second
      )
      result.map(_.shiftName) must contain(allOf("Overlap1", "Overlap2"))
    }

    "createNewShiftWhileEditing should set endDate to a day before new startDate if previous shift has no endDate and spans today" in {
      val prevShift = getStaffShiftRow.copy(endDate = None)
      val newShiftStartDate = localDateAddMonth(prevShift.startDate, 1)
      val newShift = getStaffShiftRow.copy(staffNumber = 10, startDate = newShiftStartDate)

      Await.result(dao.insertOrUpdate(prevShift), 1.second)
      val previousResult = Await.result(dao.searchStaffShift(prevShift.port, prevShift.terminal, prevShift.shiftName, prevShift.startDate), 1.second)
      val result: Shift = Await.result(dao.createNewShiftWhileEditing(prevShift, newShift), 1.second)
      val updatedPrev = Await.result(dao.searchStaffShift(prevShift.port, prevShift.terminal, prevShift.shiftName, prevShift.startDate), 1.second)
      previousResult.flatMap(_.endDate) must beNone //previous shift should not have endDate
      result.staffNumber === 10 //updated staff number
      val expectedEndDate = localDateAddDays(newShiftStartDate, -1)
      updatedPrev.flatMap(_.endDate) must beSome(expectedEndDate)

    }

    "createNewShiftWhileEditing should update previous shift endDate to start date minus one day of new shift and also latestStaffShiftForADate for different shiftDates" in {
      val prevShiftInAug = getStaffShiftRow.copy(startDate = LocalDate(2025, 8, 1), endDate = None)
      val newShiftName = "OctoberShift"
      val newShiftRequestedWithOctStartDate = getStaffShiftRow.copy(startDate = LocalDate(2025, 10, 1), endDate = None, shiftName = newShiftName)
      Await.result(dao.insertOrUpdate(prevShiftInAug), 1.second)
      val updatedShiftsResults = Await.result(dao.createNewShiftWhileEditing(prevShiftInAug, newShiftRequestedWithOctStartDate), 1.second)
      val prevShiftInAugResultAfterUpdates = Await.result(dao.latestStaffShiftForADate(prevShiftInAug.port, prevShiftInAug.terminal, prevShiftInAug.startDate, prevShiftInAug.startTime), 1.second)
      val newShiftResultWithOctStartDate = Await.result(dao.latestStaffShiftForADate(newShiftRequestedWithOctStartDate.port, newShiftRequestedWithOctStartDate.terminal, newShiftRequestedWithOctStartDate.startDate, newShiftRequestedWithOctStartDate.startTime), 1.second)

      val searchShiftForStartDateInSep = getStaffShiftRow.copy(startDate = LocalDate(2025, 9, 1))
      val searchResultForStartDateInSep = Await.result(dao.latestStaffShiftForADate(searchShiftForStartDateInSep.port, searchShiftForStartDateInSep.terminal, searchShiftForStartDateInSep.startDate, searchShiftForStartDateInSep.startTime), 1.second)
      val searchShiftForStartDateInNov = getStaffShiftRow.copy(startDate = LocalDate(2025, 11, 1))
      val searchResultForStartDateInNov = Await.result(dao.latestStaffShiftForADate(searchShiftForStartDateInNov.port, searchShiftForStartDateInNov.terminal, searchShiftForStartDateInNov.startDate, searchShiftForStartDateInNov.startTime), 1.second)

      updatedShiftsResults.shiftName === newShiftName
      prevShiftInAugResultAfterUpdates.get === prevShiftInAug.copy(endDate = Some(LocalDate(2025, 9, 30))) // Has endDate set to the day before new shift start date
      newShiftResultWithOctStartDate.isDefined === true
      newShiftResultWithOctStartDate.get === newShiftRequestedWithOctStartDate.copy(createdAt = newShiftResultWithOctStartDate.get.createdAt)
      searchResultForStartDateInSep.get === prevShiftInAugResultAfterUpdates.get // Should return the updated previous shift with endDate set
      searchResultForStartDateInNov.get === newShiftResultWithOctStartDate.get // Should return the new shift with startDate in October
    }

    "latestStaffShiftForADate for startDate in the future with start time change to 1 hour ahead" in {
      val prevShiftInAug = getStaffShiftRow.copy(startDate = LocalDate(2025, 8, 1), startTime = "09:00", endDate = None)
      Await.result(dao.insertOrUpdate(prevShiftInAug), 1.second)
      val newShiftName = "OctoberShift"
      val newShiftRequestedWithOctStartDate = getStaffShiftRow.copy(startDate = LocalDate(2025, 10, 1), endDate = None, shiftName = newShiftName, startTime = "10:00")
      val updatedShiftsResults = Await.result(dao.createNewShiftWhileEditing(prevShiftInAug, newShiftRequestedWithOctStartDate), 1.second)
      val searchShiftForStartDateInOct = getStaffShiftRow.copy(startDate = LocalDate(2025, 10, 1))
      val searchResultForStartDateInOct = Await.result(dao.latestStaffShiftForADate(searchShiftForStartDateInOct.port, searchShiftForStartDateInOct.terminal, searchShiftForStartDateInOct.startDate, searchShiftForStartDateInOct.startTime), 1.second)

      updatedShiftsResults.shiftName === newShiftName
      searchResultForStartDateInOct.get === newShiftRequestedWithOctStartDate.copy(createdAt = searchResultForStartDateInOct.get.createdAt)
    }

    "latestStaffShiftForADate for startDate in the future with start time change 1 hour behind" in {
      val prevShiftInAug = getStaffShiftRow.copy(startDate = LocalDate(2025, 8, 1), startTime = "09:00", endDate = None)
      Await.result(dao.insertOrUpdate(prevShiftInAug), 1.second)
      val newShiftName = "OctoberShift"
      val newShiftRequestedWithOctStartDate = getStaffShiftRow.copy(startDate = LocalDate(2025, 10, 1), endDate = None, shiftName = newShiftName, startTime = "08:00")
      val updatedShiftsResults = Await.result(dao.createNewShiftWhileEditing(prevShiftInAug, newShiftRequestedWithOctStartDate), 1.second)
      val searchShiftForStartDateInOct = getStaffShiftRow.copy(startDate = LocalDate(2025, 10, 1))
      val searchResultForStartDateInOct = Await.result(dao.latestStaffShiftForADate(searchShiftForStartDateInOct.port, searchShiftForStartDateInOct.terminal, searchShiftForStartDateInOct.startDate, searchShiftForStartDateInOct.startTime), 1.second)

      updatedShiftsResults.shiftName === newShiftName
      searchResultForStartDateInOct.get === newShiftRequestedWithOctStartDate.copy(createdAt = searchResultForStartDateInOct.get.createdAt)
    }

    "latestStaffShiftForADate for startDate in the future with start time change more than 3 hours ahead" in {
      val prevShiftInAug = getStaffShiftRow.copy(startDate = LocalDate(2025, 8, 1), startTime = "09:00", endDate = None)
      Await.result(dao.insertOrUpdate(prevShiftInAug), 1.second)
      val newShiftName = "OctoberShift"
      val newShiftRequestedWithOctStartDate = getStaffShiftRow.copy(startDate = LocalDate(2025, 10, 1), endDate = None, shiftName = newShiftName, startTime = "11:59")
      val updatedShiftsResults = Await.result(dao.createNewShiftWhileEditing(prevShiftInAug, newShiftRequestedWithOctStartDate), 1.second)
      val searchShiftForStartDateInOct = getStaffShiftRow.copy(startDate = LocalDate(2025, 10, 1), startTime = "09:00")
      val searchResultForStartDateInOct = Await.result(dao.latestStaffShiftForADate(searchShiftForStartDateInOct.port, searchShiftForStartDateInOct.terminal, searchShiftForStartDateInOct.startDate, searchShiftForStartDateInOct.startTime), 1.second)

      updatedShiftsResults.shiftName === newShiftName
      searchResultForStartDateInOct.get === newShiftRequestedWithOctStartDate.copy(createdAt = searchResultForStartDateInOct.get.createdAt)
    }

    "latestStaffShiftForADate for startDate in the future with start time change more than 3 hours behind" in {
      val prevShiftInAug = getStaffShiftRow.copy(startDate = LocalDate(2025, 8, 1), startTime = "09:00", endDate = None)
      Await.result(dao.insertOrUpdate(prevShiftInAug), 1.second)
      val newShiftName = "OctoberShift"
      val newShiftRequestedWithOctStartDate = getStaffShiftRow.copy(startDate = LocalDate(2025, 10, 1), endDate = None, shiftName = newShiftName, startTime = "06:00")
      val updatedShiftsResults = Await.result(dao.createNewShiftWhileEditing(prevShiftInAug, newShiftRequestedWithOctStartDate), 1.second)
      val searchShiftForStartDateInOct = getStaffShiftRow.copy(startDate = LocalDate(2025, 10, 1), startTime = "09:00")
      val searchResultForStartDateInOct = Await.result(dao.latestStaffShiftForADate(searchShiftForStartDateInOct.port, searchShiftForStartDateInOct.terminal, searchShiftForStartDateInOct.startDate, searchShiftForStartDateInOct.startTime), 1.second)

      updatedShiftsResults.shiftName === newShiftName
      searchResultForStartDateInOct.get === newShiftRequestedWithOctStartDate.copy(createdAt = searchResultForStartDateInOct.get.createdAt)
    }

    "updateStaffShift should update previous shift end date and remove future shifts and insert the new shift" in {
      val previousShift = getStaffShiftRow.copy(port = "LHR", terminal = "T1", shiftName = "Early-Aug", startDate = LocalDate(2024, 8, 1), startTime = "08:00", endDate = Option(LocalDate(2024, 9, 30)), createdAt = 0L)
      val futureExistingShift = getStaffShiftRow.copy(port = "LHR", terminal = "T1", shiftName = "Early-Oct", startDate = LocalDate(2024, 10, 1), "08:00", endDate = None, createdAt = 0L)
      val shiftRow = getStaffShiftRow.copy(port = "LHR", terminal = "T1", shiftName = "Early-Sep", startDate = LocalDate(2024, 9, 1), endDate = None, startTime = "09:00", createdAt = 0L)

      Await.result(dao.insertOrUpdate(previousShift), 1.second)
      Await.result(dao.insertOrUpdate(futureExistingShift), 1.second)
      val updatedShiftsResults = Await.result(dao.updateStaffShift(previousShift, futureExistingShift, shiftRow), 1.second)

      updatedShiftsResults.shiftName === "Early-Sep"
      updatedShiftsResults.startDate === LocalDate(2024, 9, 1)
      updatedShiftsResults.startTime === "09:00"
      updatedShiftsResults.endDate must beNone

      val updatedPreviousShift = Await.result(dao.searchStaffShift(previousShift.port, previousShift.terminal, previousShift.shiftName, previousShift.startDate), 1.second)
      updatedPreviousShift.get.shiftName === "Early-Aug"
      updatedPreviousShift.get.startDate === LocalDate(2024, 8, 1)
      updatedPreviousShift.get.endDate must beSome(LocalDate(2024, 8, 31)) // End date should be set to the day before new shift start date

      val updatedFutureShift = Await.result(dao.searchStaffShift(futureExistingShift.port, futureExistingShift.terminal, futureExistingShift.shiftName, futureExistingShift.startDate), 1.second)
      updatedFutureShift must beNone


    }
  }
}