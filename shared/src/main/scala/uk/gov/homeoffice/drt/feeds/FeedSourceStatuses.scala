package uk.gov.homeoffice.drt.feeds

import uk.gov.homeoffice.drt.ports.FeedSource
import upickle.default.{macroRW, ReadWriter => RW}

case class FeedSourceStatuses(feedSource: FeedSource, feedStatuses: FeedStatuses) {
  def name: String = feedSource.name
}

object FeedSourceStatuses {
  implicit val rw: RW[FeedSourceStatuses] = macroRW
}
