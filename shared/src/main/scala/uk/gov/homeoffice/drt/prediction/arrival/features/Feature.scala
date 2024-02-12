package uk.gov.homeoffice.drt.prediction.arrival.features

trait Feature[T] {
  val label: String
  val prefix: String
}

trait SingleFeature[T] extends Feature[T] {
  val label: String
  val value: T => Option[Double]
}

trait OneToManyFeature[T] extends Feature[T] {
  val label: String
  val value: T => Option[String]
}
