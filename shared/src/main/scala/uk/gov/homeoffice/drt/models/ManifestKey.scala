package uk.gov.homeoffice.drt.models

import uk.gov.homeoffice.drt.arrivals.{Arrival, VoyageNumber, WithTimeAccessor}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import upickle.default._

case class ManifestKey(origin: PortCode,
                       voyageNumber: VoyageNumber,
                       scheduled: Long) extends Ordered[ManifestKey] with WithTimeAccessor {
  override def compare(that: ManifestKey): Int =
    scheduled.compareTo(that.scheduled) match {
      case 0 => origin.compare(that.origin) match {
        case 0 => voyageNumber.compare(that.voyageNumber)
        case c => c
      }
      case c => c
    }

  override def timeValue: MillisSinceEpoch = scheduled
}

object ManifestKey {

  implicit val rw: ReadWriter[ManifestKey] = macroRW

  def apply(arrival: Arrival): ManifestKey = ManifestKey(arrival.PreviousPort.getOrElse(arrival.Origin), arrival.VoyageNumber, arrival.Scheduled)

  def atTime: MillisSinceEpoch => ManifestKey = (time: MillisSinceEpoch) => ManifestKey(PortCode(""), VoyageNumber(0), time)
}
