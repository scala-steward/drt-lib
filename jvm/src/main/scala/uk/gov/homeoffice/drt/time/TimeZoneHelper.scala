package uk.gov.homeoffice.drt.time

import org.joda.time.DateTimeZone

object TimeZoneHelper {
  val europeLondonId = "Europe/London"
  val europeLondonTimeZone: DateTimeZone = DateTimeZone.forID(europeLondonId)

  val utcId = "UTC"
  val utcTimeZone: DateTimeZone = DateTimeZone.forID(utcId)
}
