package uk.gov.homeoffice.drt.models

case class UserPreferences(userSelectedPlanningTimePeriod: Int,
                           hidePaxDataSourceDescription: Boolean,
                           showStaffingShiftView: Boolean,
                           desksAndQueuesIntervalMinutes: Int,
                           portDashboardIntervalMinutes: Map[String, Int],
                           portDashboardTerminals: Map[String, Set[String]])

object UserPreferences {

  def serializeMap[K, V](data: Map[K, V], valueToString: V => String): String = {
    data.map { case (key, value) => s"$key:${valueToString(value)}" }.mkString(";")
  }

  def deserializeMap[V](data: Option[String], valueParser: String => V): Map[String, V] = {
    data match {
      case Some(s) if s.nonEmpty =>
        s.split(";").map(_.split(":") match {
          case Array(key, value) if key.nonEmpty && value.nonEmpty => key -> valueParser(value)
          case Array(key) => key -> valueParser("")
          case _ => throw new IllegalArgumentException(s"Invalid format: $s")
        }).toMap
      case _ => Map.empty[String, V]
    }
  }

}
