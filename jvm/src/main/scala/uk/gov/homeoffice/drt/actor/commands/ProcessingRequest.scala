package uk.gov.homeoffice.drt.actor.commands

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import uk.gov.homeoffice.drt.time._

import scala.collection.immutable.NumericRange
import scala.concurrent.duration.{DurationLong, FiniteDuration}


case class TerminalUpdateRequest(terminal: Terminal, date: LocalDate) extends Ordered[TerminalUpdateRequest] {
  lazy val start: SDateLike = SDate(date)
  lazy val end: SDateLike = start.addDays(1).addMinutes(-1)
  lazy val duration: FiniteDuration = (end.millisSinceEpoch - start.millisSinceEpoch).milliseconds
  lazy val minutesInMillis: NumericRange[MillisSinceEpoch] = start.millisSinceEpoch to end.millisSinceEpoch by 60000
  def minutesInMillis(offsetMinutes: Int): NumericRange[MillisSinceEpoch] =
    start.addMinutes(offsetMinutes).millisSinceEpoch to end.addMinutes(offsetMinutes).millisSinceEpoch by 60000

  override def compare(that: TerminalUpdateRequest): Int = {
    val dateVal = date.compareTo(that.date) * 10
    val terminalVal = terminal.compare(that.terminal)
    dateVal + terminalVal
  }
}

case class RemoveProcessingRequest(request: TerminalUpdateRequest)

case class CrunchRequest(date: LocalDate)

case class MergeArrivalsRequest(date: UtcDate)
