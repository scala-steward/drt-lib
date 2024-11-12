package uk.gov.homeoffice.drt.time

import uk.gov.homeoffice.drt.time.MilliTimes.oneDayMillis

import scala.concurrent.duration.FiniteDuration

trait SDateLike {

  import MonthStrings._

  def ddMMyyString: String = f"$getDate%02d/$getMonth%02d/${getFullYear - 2000}%02d"

  def `DD-Month-YYYY`: String = f"$getDate%02d $getMonthString $getFullYear%04d"

  def `dayOfWeek-DD-MMM-YYYY`: String = f"$getDayOfWeekString $getDate%d ${getMonthString.substring(0, 3)} $getFullYear%04d"

  def `shortDayOfWeek-DD-MMM-YYYY`: String = f"${getDayOfWeekString.substring(0, 3)} $getDate%d ${getMonthString.substring(0, 3)} $getFullYear%04d"

  def <(other: SDateLike): Boolean = millisSinceEpoch < other.millisSinceEpoch

  def >(other: SDateLike): Boolean = millisSinceEpoch > other.millisSinceEpoch

  def -(duration: FiniteDuration): SDateLike = addMillis(-1 * duration.toMillis)

  /**
   * Days of the week 1 to 7 (Monday is 1)
   *
   * @return
   */
  def getDayOfWeek: Int

  def getFullYear: Int

  def getMonth: Int

  def getMonthString: String = months.toList(getMonth - 1)

  def getDate: Int

  def getHours: Int

  def getMinutes: Int

  def getSeconds: Int

  def millisSinceEpoch: Long

  def millisSinceEpochToMinuteBoundary: Long = millisSinceEpoch - (millisSinceEpoch % 60000)

  def toISOString: String

  def addDays(daysToAdd: Int): SDateLike

  def addMonths(monthsToAdd: Int): SDateLike

  def addHours(hoursToAdd: Int): SDateLike

  def addMinutes(minutesToAdd: Int): SDateLike

  def addMillis(millisToAdd: Int): SDateLike

  def addMillis(millisToAdd: Long): SDateLike = addMillis(millisToAdd.toInt)

  def roundToMinute(): SDateLike = {
    val remainder = millisSinceEpoch % 60000
    addMillis(-1 * remainder.toInt)
  }

  def toLocalDateTimeString: String

  def toLocalDate: LocalDate

  def toUtcDate: UtcDate

  def toISODateOnly: String = f"$getFullYear-$getMonth%02d-$getDate%02d"

  def toHoursAndMinutes: String = f"$getHours%02d:$getMinutes%02d"

  def prettyDateTime: String = f"$getDate%02d-$getMonth%02d-$getFullYear $getHours%02d:$getMinutes%02d"

  def prettyTime: String = f"$getHours%02d:$getMinutes%02d"

  def prettyTimeWithMeridian: String = {
    val hourIn12Format = if (getHours % 12 == 0) 12 else getHours % 12
    f"$hourIn12Format%02d:$getMinutes%02d ${if (getHours >= 12) "PM" else "AM"}"
  }

  def hms: String = f"$getHours%02d:$getMinutes%02d:$getSeconds%02d"

  def getZone: String

  def getTimeZoneOffsetMillis: Long

  def startOfTheMonth: SDateLike

  def getUtcLastMidnight: SDateLike

  def getLocalLastMidnight: SDateLike

  def getLocalNextMidnight: SDateLike

  def toIsoMidnight = s"$getFullYear-$getMonth-${getDate}T00:00"

  def getLastSunday: SDateLike =
    if (getDayOfWeek == 7)
      this
    else
      addDays(-1 * getDayOfWeek)

  def getDayOfWeekString: String = {
    val daysOfWeek = List("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    daysOfWeek(getDayOfWeek - 1)
  }

  override def toString: String = f"$getFullYear-$getMonth%02d-$getDate%02dT$getHours%02d$getMinutes%02d"

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case d: SDateLike =>
        d.millisSinceEpoch == millisSinceEpoch
      case _ => false
    }
  }

  def compare(that: SDateLike): Int = millisSinceEpoch.compare(that.millisSinceEpoch)

  def <=(compareTo: SDateLike): Boolean = millisSinceEpoch <= compareTo.millisSinceEpoch

  def <=(compareTo: Long): Boolean = millisSinceEpoch <= compareTo

  def >=(compareTo: SDateLike): Boolean = millisSinceEpoch >= compareTo.millisSinceEpoch

  def >=(compareTo: Long): Boolean = millisSinceEpoch >= compareTo

  def daysBetweenInclusive(that: SDateLike): Int = ((millisSinceEpoch - that.millisSinceEpoch) / oneDayMillis).abs.toInt + 1

  def isHistoricDate(now: SDateLike): Boolean = millisSinceEpoch < now.getLocalLastMidnight.millisSinceEpoch
}
