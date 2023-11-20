package uk.gov.homeoffice.drt.time

import uk.gov.homeoffice.drt.arrivals.WithTimeAccessor
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import upickle.default._


case class MilliDate(_millisSinceEpoch: MillisSinceEpoch) extends Ordered[MilliDate] with WithTimeAccessor {
  private lazy val secondsOffset: MillisSinceEpoch = _millisSinceEpoch % 60000

  lazy val millisSinceEpoch: MillisSinceEpoch = _millisSinceEpoch - secondsOffset

  override def compare(that: MilliDate): Int = _millisSinceEpoch.compare(that._millisSinceEpoch)

  override def timeValue: MillisSinceEpoch = _millisSinceEpoch
}

object MilliDate {
  implicit val rw: ReadWriter[MilliDate] = macroRW

  type MillisSinceEpoch = Long

  def atTime: MillisSinceEpoch => MilliDate = (time: MillisSinceEpoch) => MilliDate(time)
}
