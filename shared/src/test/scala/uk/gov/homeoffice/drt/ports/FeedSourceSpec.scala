package uk.gov.homeoffice.drt.ports

import org.specs2.mutable.Specification

class FeedSourceSpec extends Specification {

  "ACL feedSource object is retrieved" >> {

    "When 'ACL Random' is passed as text for feedSource" >> {
      val feedSource = FeedSource("ACL Random")

      feedSource === None
    }

    "When 'ACL' is passed as text for feedSource" >> {
      val feedSource = FeedSource("ACL")

      feedSource === Some(AclFeedSource)
    }

    "when 'AclFeedSource' is passed as text for feedSource" >> {
      val feedSource = FeedSource("AclFeedSource")

      feedSource === Some(AclFeedSource)
    }
  }
}
