package uk.gov.homeoffice.drt.time
import org.specs2.mutable.Specification

class SDateSpec extends Specification {
  "Given a date of 2024-11-01T01:00:00+01:00" >> {
    "When I ask for the date including day of week format" >> {
      "I should get Friday 1 Nov 2024" >> {
        val date = SDate("2024-11-01T01:00:00+01:00")
        date.`dayOfWeek-DD-MMM-YYYY` == "Friday 1 Nov 2024"
      }
    }
    "When I ask for the date including day of week short format" >> {
      "I should get Fri 1 Nov 2024" >> {
        val date = SDate("2024-11-01T01:00:00+01:00")
        date.`shortDayOfWeek-DD-MMM-YYYY` == "Fri 1 Nov 2024"
      }
    }
  }
}
