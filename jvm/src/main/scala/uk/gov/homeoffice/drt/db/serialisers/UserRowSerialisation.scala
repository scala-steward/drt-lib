package uk.gov.homeoffice.drt.db.serialisers

import uk.gov.homeoffice.drt.db.tables.UserRow
import uk.gov.homeoffice.drt.models.UserPreferences
import uk.gov.homeoffice.drt.models.UserPreferences.deserializeMap

object UserRowSerialisation {
  def toUserPreferences(userRow: UserRow): UserPreferences =
    UserPreferences(
      userSelectedPlanningTimePeriod = userRow.staff_planning_interval_minutes.getOrElse(60),
      hidePaxDataSourceDescription = userRow.hide_pax_data_source_description.getOrElse(false),
      showStaffingShiftView = userRow.show_staffing_shift_view.getOrElse(false),
      desksAndQueuesIntervalMinutes = userRow.desks_and_queues_interval_minutes.getOrElse(15),
      portDashboardIntervalMinutes = deserializeMap(userRow.port_dashboard_interval_minutes, _.toInt),
      portDashboardTerminals = deserializeMap(userRow.port_dashboard_terminals, _.split(",").toSet)
    )
}
