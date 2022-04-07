package uk.gov.homeoffice.drt.prediction

case class RegressionModel(coefficients: Iterable[Double], intercept: Double)
