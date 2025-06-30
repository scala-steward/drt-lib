package uk.gov.homeoffice.drt.db.tables

import slick.jdbc.PostgresProfile.api._

import java.sql.Timestamp

case class EgateSimulationRow(uuid: String,
                              startDate: Timestamp,
                              endDate: Timestamp,
                              terminal: String,
                              uptakePercentage: Double,
                              parentChildRatio: Double,
                              status: String,
                              csvContent: Option[String],
                              averageDifference: Option[Double] = None,
                              standardDeviation: Option[Double] = None,
                              createdAt: Timestamp,
                             )


class EgateSimulationTable(tag: Tag) extends Table[EgateSimulationRow](tag, "egate_simulation") {
  def uuid: Rep[String] = column[String]("id", O.Length(255, varying = true))

  def startDate: Rep[Timestamp] = column[Timestamp]("start_date")

  def endDate: Rep[Timestamp] = column[Timestamp]("end_date")

  def terminal: Rep[String] = column[String]("terminal", O.Length(64, varying = true))

  def uptakePercentage: Rep[Double] = column[Double]("uptake_percentage")

  def parentChildRatio: Rep[Double] = column[Double]("parent_child_ratio")

  def status: Rep[String] = column[String]("status", O.Length(64, varying = true))

  def csvContent: Rep[Option[String]] = column[Option[String]]("csv_content")

  def averageDifference: Rep[Option[Double]] = column[Option[Double]]("average_difference")

  def standardDeviation: Rep[Option[Double]] = column[Option[Double]]("standard_deviation")

  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at")


  def * = (uuid, startDate, endDate, terminal, uptakePercentage, parentChildRatio, status, csvContent, averageDifference, standardDeviation, createdAt) <> (EgateSimulationRow.tupled, EgateSimulationRow.unapply)

  val key = primaryKey("egate_simulation_idx", (startDate, endDate, terminal, uptakePercentage, parentChildRatio))

  val uuidIndex = index("egate_simulation_parameters_uuid", uuid, unique = true)
}
