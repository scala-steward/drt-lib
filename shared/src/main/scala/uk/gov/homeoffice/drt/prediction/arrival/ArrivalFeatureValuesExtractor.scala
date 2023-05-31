package uk.gov.homeoffice.drt.prediction.arrival

import cats.implicits.toTraverseOps
import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{Feature, OneToMany, Single}

object ArrivalFeatureValuesExtractor {
  def oneToManyFeatureValues[T](arrival: T, features: Seq[Feature[_]]): Option[Seq[String]] =
    features.collect {
      case feature: OneToMany[T] => feature.value(arrival).map(value => s"${feature.prefix}_$value")
    }.traverse(identity)

  def singleFeatureValues[T](arrival: T, features: Seq[Feature[_]]): Option[Seq[Double]] =
    features.collect {
      case feature: Single[T] => feature.value(arrival)
    }.traverse(identity)

  val minutesOffSchedule: Seq[Feature[Arrival]] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
    case arrival: Arrival =>
      for {
        touchdown <- arrival.Actual
        oneToManyValues <- oneToManyFeatureValues(arrival, features)
        singleValues <- singleFeatureValues(arrival, features)
      } yield {
        val minutes = (touchdown - arrival.Scheduled).toDouble / 60000
        (minutes, oneToManyValues, singleValues)
      }
    case unexpected =>
      scribe.error(s"Unexpected message type ${unexpected.getClass} in minutesOffSchedule")
      None
  }

  val minutesToChox: Seq[Feature[Arrival]] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
    case arrival: Arrival =>
      for {
        touchdown <- arrival.Actual
        actualChox <- arrival.ActualChox
        oneToManyValues <- oneToManyFeatureValues(arrival, features)
        singleValues <- singleFeatureValues(arrival, features)
      } yield {
        val minutes = (actualChox - touchdown).toDouble / 60000
        (minutes, oneToManyValues, singleValues)
      }
    case unexpected =>
      scribe.error(s"Unexpected message type ${unexpected.getClass} in minutesToChox")
      None
  }

  def walkTimeMinutes(walkTimeProvider: (Terminal, String, String) => Option[Int],
                     ): Seq[Feature[Arrival]] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
    case arrival: Arrival =>
      for {
        walkTimeMinutes <- walkTimeProvider(arrival.Terminal, arrival.Gate.getOrElse(""), arrival.Stand.getOrElse(""))
        oneToManyValues <- oneToManyFeatureValues(arrival, features)
        singleValues <- singleFeatureValues(arrival, features)
      } yield {
        (walkTimeMinutes.toDouble, oneToManyValues, singleValues)
      }

    case unexpected =>
      scribe.error(s"Unexpected message type ${unexpected.getClass} in walkTimeMinutes")
      None
  }

  val passengerCount: Seq[Feature[Arrival]] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
    case arrival: Arrival =>
      for {
        oneToManyValues <- oneToManyFeatureValues(arrival, features)
        singleValues <- singleFeatureValues(arrival, features)
        paxCount <- arrival.bestPcpPaxEstimate
      } yield {
        (paxCount.toDouble, oneToManyValues, singleValues)
      }

    case unexpected =>
      scribe.error(s"Unexpected message type ${unexpected.getClass} in passengerCount")
      None
  }
}
