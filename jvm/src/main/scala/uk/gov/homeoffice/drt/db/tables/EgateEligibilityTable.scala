package uk.gov.homeoffice.drt.db.tables

import slick.jdbc.PostgresProfile.api._

import java.sql.Timestamp

case class EgateEligibilityRow(port: String,
                               terminal: String,
                               dateUtc: String,
                               totalPassengers: Int,
                               egatePassengers: Int,
                               egateUnderAgePassengers: Int,
                               createdAt: Timestamp,
                              )


class EgateEligibilityTable(tag: Tag) extends Table[EgateEligibilityRow](tag, "egate_eligibility") {
  def port: Rep[String] = column[String]("port", O.Length(64, varying = true))

  def terminal: Rep[String] = column[String]("terminal", O.Length(64, varying = true))

  def dateUtc: Rep[String] = column[String]("date_utc")

  def totalPassengers: Rep[Int] = column[Int]("total_passengers")

  def egatePassengers: Rep[Int] = column[Int]("egate_passengers")

  def egateUnderAgePassengers: Rep[Int] = column[Int]("egate_under_age_passengers")

  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at")


  def * = (port, terminal, dateUtc, totalPassengers, egatePassengers, egateUnderAgePassengers, createdAt) <> (EgateEligibilityRow.tupled, EgateEligibilityRow.unapply)

  val key = primaryKey("egate_eligibility_idx", (port, terminal, dateUtc))
}
