package uk.gov.homeoffice.drt.models

import uk.gov.homeoffice.drt.Nationality
import uk.gov.homeoffice.drt.models.DocumentType.Passport
import uk.gov.homeoffice.drt.ports.{PaxAge, PortCode}

case class EeaFlag(value: String)

case class InTransit(isInTransit: Boolean) {
  override def toString: String = if (isInTransit) "Y" else "N"
}

object InTransit {
  def apply(inTransitString: String): InTransit = InTransit(inTransitString == "Y")
}

case class PassengerInfoJson(DocumentType: Option[DocumentType],
                             DocumentIssuingCountryCode: Nationality,
                             EEAFlag: EeaFlag,
                             Age: Option[PaxAge] = None,
                             DisembarkationPortCode: Option[PortCode],
                             InTransitFlag: InTransit = InTransit(false),
                             DisembarkationPortCountryCode: Option[Nationality] = None,
                             NationalityCountryCode: Option[Nationality] = None,
                             PassengerIdentifier: Option[String]
                            ) {
  def isInTransit(portCode: PortCode): Boolean = InTransitFlag.isInTransit || DisembarkationPortCode.exists(_ != portCode)

  def docTypeWithNationalityAssumption: Option[DocumentType] = NationalityCountryCode match {
    case Some(Nationality(code)) if code == CountryCodes.UK => Option(Passport)
    case _ => DocumentType
  }
}
