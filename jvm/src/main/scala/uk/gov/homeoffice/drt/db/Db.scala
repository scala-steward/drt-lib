package uk.gov.homeoffice.drt.db

import scala.concurrent.Future


trait CentralDatabase {
  val profile: slick.jdbc.JdbcProfile
  val db: profile.backend.Database

  def run[T](action: profile.api.DBIOAction[T, profile.api.NoStream, Nothing]): Future[T] = {
    db.run(action)
  }
}
