package uk.gov.homeoffice.drt.db

import slick.dbio.{DBIOAction, NoStream}

import scala.concurrent.Future


trait CentralDatabase {
  val profile: slick.jdbc.JdbcProfile
  val db: profile.backend.Database

  def run[R](action: DBIOAction[R, NoStream, Nothing]): Future[R] = db.run[R](action)
}
