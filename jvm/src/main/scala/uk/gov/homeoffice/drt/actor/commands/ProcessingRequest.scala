package uk.gov.homeoffice.drt.actor.commands

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import uk.gov.homeoffice.drt.time.TimeZoneHelper.europeLondonTimeZone
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

import scala.collection.immutable.NumericRange


sealed trait ProcessingRequest extends Ordered[ProcessingRequest] {
  val localDate: LocalDate
  val offsetMinutes: Int
  val durationMinutes: Int
  lazy val start: SDateLike = SDate(localDate).addMinutes(offsetMinutes)
  lazy val end: SDateLike = start.addMinutes(durationMinutes)
  lazy val minutesInMillis: NumericRange[MillisSinceEpoch] = start.millisSinceEpoch until end.millisSinceEpoch by 60000

  override def compare(that: ProcessingRequest): Int =
    if (localDate < that.localDate) -1
    else if (localDate > that.localDate) 1
    else 0
}

case class TerminalUpdateRequest(terminal: Terminal, localDate: LocalDate, offsetMinutes: Int, durationMinutes: Int) extends ProcessingRequest

case class RemoveCrunchRequest(crunchRequest: ProcessingRequest)

case class CrunchRequest(localDate: LocalDate, offsetMinutes: Int, durationMinutes: Int) extends ProcessingRequest

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
