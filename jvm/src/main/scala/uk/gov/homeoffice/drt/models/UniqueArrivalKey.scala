package uk.gov.homeoffice.drt.models

import uk.gov.homeoffice.drt.arrivals.{Arrival, FeedArrival, UniqueArrival, VoyageNumber}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

case class UniqueArrivalKey(arrivalPort: PortCode,
                            departurePort: PortCode,
                            voyageNumber: VoyageNumber,
                            scheduled: SDateLike) {
  override def toString: String = s"$departurePort -> $arrivalPort: $voyageNumber @ ${scheduled.toISOString}"
}

object UniqueArrivalKey {
  def apply(arrival: Arrival, port: PortCode): UniqueArrivalKey =
    UniqueArrivalKey(port, arrival.Origin, arrival.VoyageNumber, SDate(arrival.Scheduled))

  def apply(portCode: PortCode, arrivalKey: UniqueArrival): UniqueArrivalKey =
    UniqueArrivalKey(portCode, arrivalKey.origin, VoyageNumber(arrivalKey.number), SDate(arrivalKey.scheduled))

  def apply(feedArrival: FeedArrival, port: PortCode): UniqueArrivalKey =
    UniqueArrivalKey(port, PortCode(feedArrival.origin), VoyageNumber(feedArrival.voyageNumber), SDate(feedArrival.scheduled))
}
