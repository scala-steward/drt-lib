package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.arrivals.{Arrival, Passengers}
import uk.gov.homeoffice.drt.ports.MlFeedSource
import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, RegressionModel}
import uk.gov.homeoffice.drt.time.SDateLike

object PaxCapModelAndFeatures {
  val targetName: String = "paxcap"
  val featuresVersion: Int = 1
}

case class PaxCapModelAndFeatures(model: RegressionModel,
                                  features: FeaturesWithOneToManyValues,
                                  examplesTrainedOn: Int,
                                  improvementPct: Double,
                                 ) extends ArrivalModelAndFeatures {
  override val featuresVersion: Int = PaxCapModelAndFeatures.featuresVersion
  override val targetName: String = PaxCapModelAndFeatures.targetName

  private val fallback: Int = 175

  override def updatePrediction(arrival: Arrival, minimumImprovementPctThreshold: Int, upperThreshold: Option[Int], now: SDateLike): Arrival = {
    val updatedPassengers = maybePrediction(arrival, minimumImprovementPctThreshold, upperThreshold) match {
      case None =>
        arrival.PassengerSources.removed(MlFeedSource)
      case Some(pctFull) =>
        val pax = arrival.MaxPax
          .map { maxPax =>
            ((pctFull.toDouble / 100) * maxPax).toInt
          }
          .getOrElse({
            scribe.warn(s"Unknown capacity for ${arrival.flightCode} @ ${arrival.Scheduled}. Using fallback value of $fallback")
            fallback
          })
        arrival.PassengerSources.updated(MlFeedSource, Passengers(Option(pax), Option(0)))
    }

    arrival.copy(PassengerSources = updatedPassengers)
  }
}
