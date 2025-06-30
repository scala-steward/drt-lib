package uk.gov.homeoffice.drt.db.tables

import slick.jdbc.PostgresProfile.api._

import java.sql.Timestamp

case class EgateSimulationRow(uuid: String,
                              port: String,
                              terminal: String,
                              startDate: Timestamp,
                              endDate: Timestamp,
                              uptakePercentage: Double,
                              parentChildRatio: Double,
                              status: String,
                              csvContent: Option[String],
                              meanAbsolutePercentageError: Option[Double],
                              standardDeviation: Option[Double],
                              bias: Option[Double],
                              correlationCoefficient: Option[Double],
                              rSquaredError: Option[Double],
                              createdAt: Timestamp,
                             )


class EgateSimulationTable(tag: Tag) extends Table[EgateSimulationRow](tag, "egate_simulation") {
  def uuid: Rep[String] = column[String]("id", O.Length(255, varying = true))

  def port: Rep[String] = column[String]("port", O.Length(64, varying = true))

  def terminal: Rep[String] = column[String]("terminal", O.Length(64, varying = true))

  def startDate: Rep[Timestamp] = column[Timestamp]("start_date")

  def endDate: Rep[Timestamp] = column[Timestamp]("end_date")

  def uptakePercentage: Rep[Double] = column[Double]("uptake_percentage")

  def parentChildRatio: Rep[Double] = column[Double]("parent_child_ratio")

  def status: Rep[String] = column[String]("status", O.Length(64, varying = true))

  def csvContent: Rep[Option[String]] = column[Option[String]]("csv_content")

  def meanAbsolutePercentageError: Rep[Option[Double]] = column[Option[Double]]("mean_absolute_percentage_error")

  def standardDeviation: Rep[Option[Double]] = column[Option[Double]]("standard_deviation")

  def bias: Rep[Option[Double]] = column[Option[Double]]("bias")

  def correlationCoefficient: Rep[Option[Double]] = column[Option[Double]]("correlation_coefficient")

  def rSquaredError: Rep[Option[Double]] = column[Option[Double]]("r_squared_error")

  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at")


  def * = (uuid, port, terminal, startDate, endDate, uptakePercentage, parentChildRatio, status, csvContent,
    meanAbsolutePercentageError, standardDeviation, bias, correlationCoefficient, rSquaredError, createdAt) <> (EgateSimulationRow.tupled, EgateSimulationRow.unapply)

  val key = primaryKey("egate_simulation_idx", (startDate, endDate, terminal, uptakePercentage, parentChildRatio))

  val uuidIndex = index("egate_simulation_parameters_uuid", uuid, unique = true)
}
