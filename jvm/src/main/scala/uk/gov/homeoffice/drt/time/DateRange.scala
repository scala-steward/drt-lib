package uk.gov.homeoffice.drt.time

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

object DateRange {
  type MillisToDateLike[A <: DateLike] = Long => A

  val millisToUtc: MillisToDateLike[UtcDate] = (millis: Long) => SDate(millis).toUtcDate
  val millisToLocal: MillisToDateLike[LocalDate] = (millis: Long) => SDate(millis).toLocalDate

  def dateRangeWithBuffer[A <: DateLike](startBuffer: Int, endBuffer: Int, millisToDate: MillisToDateLike[A])(start: SDateLike, end: SDateLike): Seq[A] = {
    val startMillis = start.addDays(startBuffer * -1).millisSinceEpoch
    val endMillis = end.addDays(endBuffer).millisSinceEpoch
    val daysRangeMillis = startMillis to endMillis by MilliTimes.oneDayMillis
    daysRangeMillis.map(millisToDate)
  }

  def dateRange[A <: DateLike](start: SDateLike, end: SDateLike, millisToDate: MillisToDateLike[A]): Seq[A] = {
    val startMillis = start.millisSinceEpoch
    val endMillis = end.millisSinceEpoch
    val daysRangeMillis = (startMillis to endMillis by MilliTimes.oneDayMillis) :+ end.millisSinceEpoch
    daysRangeMillis.map(millisToDate).distinct
  }

  def apply(start: LocalDate, end: LocalDate): Seq[LocalDate] =
    LazyList
      .iterate(SDate(start))(_.addDays(1))
      .takeWhile(_.toLocalDate <= end)
      .map(_.toLocalDate)

  def apply(start: UtcDate, end: UtcDate): Seq[UtcDate] =
    LazyList
      .iterate(SDate(start))(_.addDays(1))
      .takeWhile(_.toUtcDate <= end)
      .map(_.toUtcDate)

  def dateRangeSource[A <: DateLike](start: SDateLike, end: SDateLike, millisToDate: MillisToDateLike[A]): Source[A, NotUsed] =
    Source(dateRange(start, end, millisToDate).toList)

  def utcDateRangeWithBuffer(startBuffer: Int, endBuffer: Int)(start: SDateLike, end: SDateLike): Seq[UtcDate] =
    dateRangeWithBuffer(startBuffer, endBuffer, millisToUtc)(start, end)

  def localDateRangeWithBuffer(startBuffer: Int, endBuffer: Int)(start: SDateLike, end: SDateLike): Seq[LocalDate] =
    dateRangeWithBuffer(startBuffer, endBuffer, millisToLocal)(start, end)

  def utcDateRange(start: SDateLike, end: SDateLike): Seq[UtcDate] =
    dateRange(start, end, millisToUtc)

  def localDateRange(start: SDateLike, end: SDateLike): Seq[LocalDate] =
    dateRange(start, end, millisToLocal)

  def utcDateRangeSource(start: SDateLike, end: SDateLike): Source[UtcDate, NotUsed] =
    Source(dateRange(start, end, millisToUtc).toList)

  def localDateRangeSource(start: SDateLike, end: SDateLike): Source[LocalDate, NotUsed] =
    Source(dateRange(start, end, millisToLocal).toList)
}
