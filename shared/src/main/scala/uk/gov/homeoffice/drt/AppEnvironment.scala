package uk.gov.homeoffice.drt

object AppEnvironment {
  sealed trait AppEnvironment

  case object ProdEnv extends AppEnvironment

  case object PreProdEnv extends AppEnvironment

  case object TestEnv extends AppEnvironment

  case object OtherEnv extends AppEnvironment

  def apply(envStr: String): AppEnvironment = envStr.toLowerCase match {
    case "prod" => ProdEnv
    case "preprod" => PreProdEnv
    case "test" => TestEnv
    case _ => OtherEnv
  }
}
