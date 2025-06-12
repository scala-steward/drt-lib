package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.tables.UserRow
import uk.gov.homeoffice.drt.models.UserPreferences

class UserRowSerialisationTest extends AnyWordSpec with Matchers {
  "UserRowSerialisation" should {
    "convert UserRow to UserPreferences correctly" in {
      val userRow = UserRow(
        id = "test-id",
        username = "test-user",
        email = "test@example.com",
        latest_login = java.sql.Timestamp.valueOf("2023-01-01 00:00:00"),
        inactive_email_sent = None,
        revoked_access = None,
        drop_in_notification_at = None,
        created_at = None,
        feedback_banner_closed_at = None,
        staff_planning_interval_minutes = Some(30),
        hide_pax_data_source_description = Some(true),
        show_staffing_shift_view = Some(false),
        desks_and_queues_interval_minutes = Some(20),
        port_dashboard_interval_minutes = Some("LHR:30"),
        port_dashboard_terminals = Some("LHR:T1,T2")
      )

      val userPreferences = UserRowSerialisation.toUserPreferences(userRow)

      val expected = UserPreferences(
        userSelectedPlanningTimePeriod = 30,
        hidePaxDataSourceDescription = true,
        showStaffingShiftView = false,
        desksAndQueuesIntervalMinutes = 20,
        portDashboardIntervalMinutes = Map("LHR" -> 30),
        portDashboardTerminals = Map("LHR" -> Set("T1", "T2"))
      )

      userPreferences shouldEqual expected
    }
  }
}
