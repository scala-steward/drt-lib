package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.prediction.ModelAndFeatures
import uk.gov.homeoffice.drt.time.SDateLike


trait ArrivalModelAndFeatures extends ModelAndFeatures {
  def maybePrediction(arrival: Arrival, minimumImprovementPctThreshold: Int, upperValueThreshold: Option[Int]): Option[Int] =
    if (improvementPct > minimumImprovementPctThreshold) {
      for {
        valueThreshold <- upperValueThreshold
        value <- prediction(arrival)
        maybePrediction <- if (value.abs <= valueThreshold)
          Option(value)
        else {
          scribe.warn(s"Prediction of $value is greater than threshold of $valueThreshold. Capping at threshold")
          Option(valueThreshold)
        }
      } yield maybePrediction
    } else {
      scribe.warn(s"Improvement of $improvementPct% is less than threshold of $minimumImprovementPctThreshold%")
      None
    }

  def prediction(arrival: Arrival): Option[Int] = {
    val maybeMaybePrediction = for {
      oneToManyValues <- ArrivalFeatureValuesExtractor.oneToManyFeatureValues(arrival, features.features)
      singleValues <- ArrivalFeatureValuesExtractor.singleFeatureValues(arrival, features.features)
    } yield {
      val oneToManyFeatureValues: Seq[Option[Double]] = oneToManyValues.map { featureValue =>
        val featureIdx = features.oneToManyValues.indexOf(featureValue)
        model.coefficients.toIndexedSeq.lift(featureIdx)
      }
      val singleFeatureValues: Seq[Option[Double]] = singleValues.zipWithIndex.map { case (featureValue, idx) =>
        model.coefficients.toIndexedSeq.lift(idx).map(_ * featureValue)
      }
      val allFeatureValues = oneToManyFeatureValues ++ singleFeatureValues
      Option((model.intercept + allFeatureValues.map(_.getOrElse(0d)).sum).round.toInt)
    }
    maybeMaybePrediction.flatten
  }

  def updatePrediction(arrival: Arrival, minimumImprovementPctThreshold: Int, upperThreshold: Option[Int], now: SDateLike): Arrival = {
    val updatedPredictions = maybePrediction(arrival, minimumImprovementPctThreshold, upperThreshold) match {
      case None =>
        arrival.Predictions.copy(predictions = arrival.Predictions.predictions.removed(targetName), lastUpdated = now.millisSinceEpoch)
      case Some(update) if !arrival.Predictions.predictions.get(targetName).contains(update) =>
        arrival.Predictions.copy(predictions = arrival.Predictions.predictions.updated(targetName, update), lastUpdated = now.millisSinceEpoch)
      case Some(_) =>
        arrival.Predictions
    }
    arrival.copy(Predictions = updatedPredictions)
  }

}
