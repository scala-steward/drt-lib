package uk.gov.homeoffice.drt.db

import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

object TestDatabase {
  val profile: JdbcProfile = slick.jdbc.H2Profile
  val db: profile.backend.Database = profile.api.Database.forConfig("h2-db")
  lazy val userFeedbackTable: TableQuery[UserFeedbackTable] = TableQuery[UserFeedbackTable]
  lazy val abFeatureTable: TableQuery[ABFeatureTable] = TableQuery[ABFeatureTable]
}
