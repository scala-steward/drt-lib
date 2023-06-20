package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.arrivals.{Arrival, Passengers}
import uk.gov.homeoffice.drt.ports.MlFeedSource
import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, RegressionModel}
import uk.gov.homeoffice.drt.time.SDateLike

object PaxCapModelAndFeatures {
  val targetName: String = "paxcap"
}

case class PaxCapModelAndFeatures(model: RegressionModel,
                                  features: FeaturesWithOneToManyValues,
                                  examplesTrainedOn: Int,
                                  improvementPct: Double,
                                 ) extends ArrivalModelAndFeatures {
  override val targetName: String = PaxCapModelAndFeatures.targetName

  override def updatePrediction(arrival: Arrival, minimumImprovementPctThreshold: Int, upperThreshold: Option[Int], now: SDateLike): Arrival = {
    val updatedPassengers = maybePrediction(arrival, minimumImprovementPctThreshold, upperThreshold) match {
      case None => arrival.PassengerSources.removed(MlFeedSource)
      case Some(update) => arrival.PassengerSources.updated(MlFeedSource, Passengers(Option(update), Option(0)))
    }
    arrival.copy(PassengerSources = updatedPassengers)
  }
}
