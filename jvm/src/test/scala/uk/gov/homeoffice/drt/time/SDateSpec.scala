package uk.gov.homeoffice.drt.time

import org.joda.time.DateTimeZone
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

  "When I ask for the date including day of week with full month format" >> {
    "I should get Friday 1 November 2024" >> {
      val date = SDate("2024-11-01T01:00:00+01:00")
      date.`dayOfWeek-DD-Month-YYYY` == "Friday 1 November 2024"
    }
  }

  "firstDayOfWeek" should "return Monday for any day in the week for UTC timezone" >> {
    val zone = DateTimeZone.UTC
    // 2024-06-05 is a Wednesday
    val wednesday = SDate(2024, 6, 5, 0, 0, zone)
    val monday = SDate.firstDayOfWeek(wednesday)
    monday.getDayOfWeek == 1 &&
      monday.getDate == 3
  }

  "lastDayOfWeek" should "return Sunday for any day in the week for UTC timezone" >> {
    val zone = DateTimeZone.UTC
    // 2024-06-05 is a Wednesday
    val wednesday = SDate(2024, 6, 5, 0, 0, zone)
    val sunday = SDate.lastDayOfWeek(wednesday)
    sunday.getDayOfWeek == 7 &&
      sunday.getDate == 9
  }


  "firstDayOfWeek" should "return Monday for any day in the week for Europe/London Timezone" >> {
    val zone = DateTimeZone.forID("Europe/London")
    // 2024-06-05 is a Wednesday
    val wednesday = SDate(2024, 6, 5, 0, 0, zone)
    val monday = SDate.firstDayOfWeek(wednesday)
    monday.getDayOfWeek == 1 &&
      monday.getDate == 3
  }

  "lastDayOfWeek" should "return Sunday for any day in the week Europe/London Timezone" >> {
    val zone = DateTimeZone.forID("Europe/London")
    // 2024-06-05 is a Wednesday
    val wednesday = SDate(2024, 6, 5, 0, 0, zone)
    val sunday = SDate.lastDayOfWeek(wednesday)
    sunday.getDayOfWeek == 7 &&
      sunday.getDate == 9
  }

}
