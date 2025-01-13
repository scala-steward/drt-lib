package uk.gov.homeoffice.drt.model

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch

trait MinuteLike[A, B] {
  val minute: MillisSinceEpoch
  val lastUpdated: Option[MillisSinceEpoch]
  val terminal: Terminal

  def maybeUpdated(existing: A, now: MillisSinceEpoch): Option[A]

  val key: B

  def toUpdatedMinute(now: MillisSinceEpoch): A

  def toMinute: A
}

trait WithMinute {
  val minute: MillisSinceEpoch
}
