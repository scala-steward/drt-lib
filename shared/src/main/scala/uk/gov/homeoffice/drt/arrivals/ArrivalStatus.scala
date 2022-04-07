package uk.gov.homeoffice.drt.arrivals

case class ArrivalStatus(description: String) {
  override def toString: String = description
}
