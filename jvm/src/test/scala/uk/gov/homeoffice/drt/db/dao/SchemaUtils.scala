package uk.gov.homeoffice.drt.db.dao

object SchemaUtils {
  def printStatements(statements: Iterator[String]): Unit = println(statements.mkString(";\n") + ";")
}
