package uk.gov.homeoffice.drt.jsonformats

import spray.json._
import uk.gov.homeoffice.drt.models.UserPreferences
import uk.gov.homeoffice.drt.models.UserPreferences.{deserializeMap, serializeMap}

object UserPreferencesJsonFormat extends DefaultJsonProtocol {
  implicit val userPreferencesFormat: RootJsonFormat[UserPreferences] = new RootJsonFormat[UserPreferences] {
    def write(obj: UserPreferences): JsValue = JsObject(
      "userSelectedPlanningTimePeriod" -> JsNumber(obj.userSelectedPlanningTimePeriod),
      "hidePaxDataSourceDescription" -> JsBoolean(obj.hidePaxDataSourceDescription),
      "showStaffingShiftView" -> JsBoolean(obj.showStaffingShiftView),
      "desksAndQueuesIntervalMinutes" -> JsNumber(obj.desksAndQueuesIntervalMinutes),
      "portDashboardIntervalMinutes" -> JsString(serializeMap(obj.portDashboardIntervalMinutes, (value: Int) => value.toString)),
      "portDashboardTerminals" -> JsString(serializeMap(obj.portDashboardTerminals, (values: Set[String]) => values.mkString(","))),
    )

    def read(json: JsValue): UserPreferences = json.asJsObject.getFields(
      "userSelectedPlanningTimePeriod",
      "hidePaxDataSourceDescription",
      "showStaffingShiftView",
      "desksAndQueuesIntervalMinutes",
      "portDashboardIntervalMinutes",
      "portDashboardTerminals"
    ) match {
      case Seq(
      JsNumber(staffPlanningIntervalMinutes),
      JsBoolean(hidePaxDataSourceDescription),
      JsBoolean(showStaffingShiftView),
      JsNumber(desksAndQueuesIntervalMinutes),
      JsString(portDashboardIntervalMinutes),
      JsString(portDashboardTerminals)
      ) =>
        UserPreferences(
          staffPlanningIntervalMinutes.toInt,
          hidePaxDataSourceDescription,
          showStaffingShiftView,
          desksAndQueuesIntervalMinutes.toInt,
          deserializeMap(Some(portDashboardIntervalMinutes), _.toInt),
          deserializeMap(Some(portDashboardTerminals), _.split(",").toSet)
        )
      case _ => throw DeserializationException("Invalid UserPreferences JSON format")
    }
  }
}
