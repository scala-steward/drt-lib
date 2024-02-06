package uk.gov.homeoffice.drt.prediction.arrival

import cats.implicits.toTraverseOps
import uk.gov.homeoffice.drt.arrivals.{Arrival, ArrivalStatus, Passengers}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{ApiFeedSource, LiveFeedSource}
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{Feature, OneToMany, Single}

object ArrivalFeatureValuesExtractor {
  def oneToManyFeatureValues[T](arrival: T, features: Seq[Feature[_]]): Option[Seq[String]] =
    features.collect {
      case feature: OneToMany[T] =>
        val value: Option[String] = feature.value(arrival).map(value => s"${feature.prefix}_$value")
        if (value.isEmpty) scribe.warn(s"Feature ${feature.label} has no value for ${arrival.toString}")
        value
    }.traverse(identity)

  def singleFeatureValues[T](arrival: T, features: Seq[Feature[_]]): Option[Seq[Double]] =
    features.collect {
      case feature: Single[T] => feature.value(arrival)
    }.traverse(identity)

  val minutesOffSchedule: Seq[Feature[Arrival]] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
    arrival =>
      for {
        touchdown <- arrival.Actual
        oneToManyValues <- oneToManyFeatureValues(arrival, features)
        singleValues <- singleFeatureValues(arrival, features)
      } yield {
        val minutes = (touchdown - arrival.Scheduled).toDouble / 60000
        (minutes, oneToManyValues, singleValues)
      }
  }

  val minutesToChox: Seq[Feature[Arrival]] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
    arrival =>
      for {
        touchdown <- arrival.Actual
        actualChox <- arrival.ActualChox
        oneToManyValues <- oneToManyFeatureValues(arrival, features)
        singleValues <- singleFeatureValues(arrival, features)
      } yield {
        val minutes = (actualChox - touchdown).toDouble / 60000
        (minutes, oneToManyValues, singleValues)
      }
  }

  def walkTimeMinutes(walkTimeProvider: (Terminal, String, String) => Option[Int],
                     ): Seq[Feature[Arrival]] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
    arrival =>
      for {
        walkTimeMinutes <- walkTimeProvider(arrival.Terminal, arrival.Gate.getOrElse(""), arrival.Stand.getOrElse(""))
        oneToManyValues <- oneToManyFeatureValues(arrival, features)
        singleValues <- singleFeatureValues(arrival, features)
      } yield {
        (walkTimeMinutes.toDouble, oneToManyValues, singleValues)
      }
  }

  private def noReliablePaxCount(arrival: Arrival): Boolean = {
    !arrival.PassengerSources.exists {
      case (feedSource, Passengers(maybePax, _)) => List(ApiFeedSource, LiveFeedSource).contains(feedSource) && maybePax.nonEmpty
    }
  }

  val percentCapacity: Seq[Feature[Arrival]] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
    case arrival if arrival.MaxPax.isEmpty => None
    case arrival if noReliablePaxCount(arrival) => None
    case arrival if arrival.Status == ArrivalStatus("Cancelled") => None
    case arrival =>
      for {
        oneToManyValues <- oneToManyFeatureValues(arrival, features)
        singleValues <- singleFeatureValues(arrival, features)
        paxCount <- arrival.bestPcpPaxEstimate(List(LiveFeedSource, ApiFeedSource))
        maxPax <- arrival.MaxPax
      } yield {
        val pctFull = 100 * paxCount.toDouble / maxPax match {
          case over100 if over100 > 100 => 100
          case pct => pct
        }
        (pctFull, oneToManyValues, singleValues)
      }
  }
}
