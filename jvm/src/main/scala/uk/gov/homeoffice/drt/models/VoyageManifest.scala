package uk.gov.homeoffice.drt.models

import org.joda.time.DateTime
import uk.gov.homeoffice.drt.Nationality
import uk.gov.homeoffice.drt.arrivals.{CarrierCode, EventType, VoyageNumber, VoyageNumberLike}
import uk.gov.homeoffice.drt.ports.{PaxAge, PortCode, SplitRatiosNs}
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSource
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages
import uk.gov.homeoffice.drt.time.SDate.JodaSDate
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.util.Try

trait ManifestLike {
  val source: SplitSource
  val arrivalPortCode: PortCode
  val departurePortCode: PortCode
  val voyageNumber: VoyageNumberLike
  val carrierCode: CarrierCode
  val scheduled: SDateLike
  val nonUniquePassengers: Seq[ManifestPassengerProfile]
  val maybeEventType: Option[EventType]

  def uniquePassengers: Seq[ManifestPassengerProfile] = {
    if (nonUniquePassengers.exists(_.passengerIdentifier.exists(_.nonEmpty)))
      nonUniquePassengers
        .collect {
          case p@ManifestPassengerProfile(_, _, _, _, Some(id)) if id.nonEmpty => p
        }
        .map { passengerInfo =>
          passengerInfo.passengerIdentifier -> passengerInfo
        }
        .toMap
        .values
        .toList
    else
      nonUniquePassengers
  }

  def maybeKey: Option[ManifestKey] = voyageNumber match {
    case vn: VoyageNumber =>
      Option(ManifestKey(departurePortCode, vn, scheduled.millisSinceEpoch))
    case _ => None
  }
}

case class ManifestDateOfArrival(date: String) {
  override def toString: String = date
}

case class ManifestTimeOfArrival(time: String) {
  override def toString: String = time
}

case class VoyageManifest(EventCode: EventType,
                          ArrivalPortCode: PortCode,
                          DeparturePortCode: PortCode,
                          VoyageNumber: VoyageNumberLike,
                          CarrierCode: CarrierCode,
                          ScheduledDateOfArrival: ManifestDateOfArrival,
                          ScheduledTimeOfArrival: ManifestTimeOfArrival,
                          PassengerList: List[PassengerInfoJson]) extends ManifestLike {
  def flightCode: String = CarrierCode.code + VoyageNumber

  def scheduleArrivalDateTime: Option[SDateLike] = Try(DateTime.parse(scheduleDateTimeString)).toOption.map(JodaSDate)

  def scheduleDateTimeString: String = s"${ScheduledDateOfArrival}T${ScheduledTimeOfArrival}Z"

  override val source: SplitRatiosNs.SplitSource = ApiSplitsWithHistoricalEGateAndFTPercentages
  override val scheduled: SDateLike = scheduleArrivalDateTime.getOrElse(SDate(0L))
  override val arrivalPortCode: PortCode = ArrivalPortCode
  override val departurePortCode: PortCode = DeparturePortCode
  override val voyageNumber: VoyageNumberLike = VoyageNumber
  override val carrierCode: CarrierCode = CarrierCode
  override val nonUniquePassengers: List[ManifestPassengerProfile] = PassengerList.map(ManifestPassengerProfile(_, arrivalPortCode))
  override val maybeEventType: Option[EventType] = Option(EventCode)
}


case class ManifestPassengerProfile(nationality: Nationality,
                                    documentType: Option[DocumentType],
                                    age: Option[PaxAge],
                                    inTransit: Boolean,
                                    passengerIdentifier: Option[String])

object ManifestPassengerProfile {
  def apply(pij: PassengerInfoJson, portCode: PortCode): ManifestPassengerProfile =
    ManifestPassengerProfile(
      nationality = pij.NationalityCountryCode.getOrElse(Nationality("")),
      documentType = pij.docTypeWithNationalityAssumption,
      age = pij.Age,
      inTransit = pij.isInTransit(portCode),
      passengerIdentifier = pij.PassengerIdentifier
    )
}
