package uk.gov.homeoffice.drt.ports.config.updates

trait ConfigUpdate[A] {
  def effectiveFrom: Long

  def maybeOriginalEffectiveFrom: Option[Long]

  def configItem: A
}
