package uk.gov.homeoffice.drt.db

object TestDatabase extends CentralDatabase {
  val profile: slick.jdbc.JdbcProfile = slick.jdbc.H2Profile

  override val db: profile.backend.Database = profile.api.Database.forConfig("h2-db")
}
