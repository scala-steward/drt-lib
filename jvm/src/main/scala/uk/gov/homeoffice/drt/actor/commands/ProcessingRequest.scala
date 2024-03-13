package uk.gov.homeoffice.drt.actor.commands

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import uk.gov.homeoffice.drt.time.TimeZoneHelper.europeLondonTimeZone
import uk.gov.homeoffice.drt.time._

import scala.collection.immutable.NumericRange
import scala.concurrent.duration.DurationLong

sealed trait ProcessingRequest extends Ordered[ProcessingRequest] {
  val date: DateLike
  lazy val start: SDateLike = SDate(date)
  lazy val end: SDateLike = start.addDays(1).addMinutes(-1)
  lazy val duration = (end.millisSinceEpoch - start.millisSinceEpoch).milliseconds
  lazy val minutesInMillis: NumericRange[MillisSinceEpoch] = start.millisSinceEpoch until end.millisSinceEpoch by 60000

  override def compare(that: ProcessingRequest): Int =
    if (date < that.date) -1
    else if (date > that.date) 1
    else 0
}

sealed trait LoadProcessingRequest extends ProcessingRequest {
  val date: LocalDate
  val offsetMinutes: Int
  val durationMinutes: Int
  override lazy val start: SDateLike = SDate(date).addMinutes(offsetMinutes)
  override lazy val end: SDateLike = start.addMinutes(durationMinutes)
}

case class TerminalUpdateRequest(terminal: Terminal, date: LocalDate, offsetMinutes: Int, durationMinutes: Int) extends LoadProcessingRequest

case class RemoveProcessingRequest(request: ProcessingRequest)

case class CrunchRequest(date: LocalDate, offsetMinutes: Int, durationMinutes: Int) extends LoadProcessingRequest

case class MergeArrivalsRequest(date: UtcDate) extends ProcessingRequest

object CrunchRequest {
  def apply(millis: MillisSinceEpoch, offsetMinutes: Int, durationMinutes: Int): CrunchRequest = {
    val midnight = SDate(millis, europeLondonTimeZone)
      .addMinutes(-1 * offsetMinutes)
      .getLocalLastMidnight
    val localDate = midnight
      .toLocalDate

    CrunchRequest(localDate, offsetMinutes, durationMinutes)
  }
}
