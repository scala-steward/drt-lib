package uk.gov.homeoffice.drt.prediction.arrival

import cats.implicits.toTraverseOps
import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.prediction.Feature
import uk.gov.homeoffice.drt.prediction.Feature.{OneToMany, Single}
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{OneToManyFeatureColumn, SingleFeatureColumn}

object ArrivalFeatureValuesExtractor {
  def oneToManyColumnValues[T](arrival: T, features: Seq[Feature]): Option[Seq[String]] =
    features.collect {
      case OneToMany(columns, prefix) =>
        val maybeValues = columns.map {
          case column: OneToManyFeatureColumn[T] => column.value(arrival)
        }
        maybeValues.traverse(identity).map(vs => s"${prefix}_${vs.mkString("_")}")
    }.traverse(identity)

  def oneToManyFeatureValues[T](arrival: T, features: Seq[Feature]): Option[Seq[String]] =
    features.collect {
      case OneToMany(columns, _) =>
        columns.map {
          case column: OneToManyFeatureColumn[T] => column.value(arrival)
        }
    }.flatten.traverse(identity)

  def singleFeatureValues[T](arrival: T, features: Seq[Feature]): Option[Seq[Double]] =
    features.collect {
      case Single(column: SingleFeatureColumn[T]) => column.value(arrival)
    }.traverse(identity)

  val minutesOffSchedule: Seq[Feature] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
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

  val minutesToChox: Seq[Feature] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
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
                     ): Seq[Feature] => Arrival => Option[(Double, Seq[String], Seq[Double])] = features => {
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
}
