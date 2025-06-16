package uk.gov.homeoffice.drt.jsonformats

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spray.json.enrichAny
import uk.gov.homeoffice.drt.jsonformats.UserPreferencesJsonFormat._
import uk.gov.homeoffice.drt.models.UserPreferences

class UserPreferencesJsonFormatTest extends AnyFlatSpec with Matchers {
  it should "serialize and deserialize UserPreferences correctly" in {
    val userPreferences = UserPreferences(
      userSelectedPlanningTimePeriod = 30,
      hidePaxDataSourceDescription = true,
      showStaffingShiftView = false,
      desksAndQueuesIntervalMinutes = 15,
      portDashboardIntervalMinutes = Map("port1" -> 10, "port2" -> 20),
      portDashboardTerminals = Map("lhr" -> Set("t2", "t3"), "lgw" -> Set("N"))
    )

    val serialized = userPreferences.toJson
    val deserialized = serialized.convertTo[UserPreferences]
    deserialized shouldEqual userPreferences
  }
}
