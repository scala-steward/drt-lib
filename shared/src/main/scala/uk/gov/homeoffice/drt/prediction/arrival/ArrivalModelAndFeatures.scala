package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.prediction.ModelAndFeatures
import uk.gov.homeoffice.drt.time.SDateLike


trait ArrivalModelAndFeatures extends ModelAndFeatures {
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
      if (allFeatureValues.forall(_.isDefined)) {
        Some((model.intercept + allFeatureValues.map(_.get).sum).round.toInt)
      } else {
        None
      }
    }
    maybeMaybePrediction.flatten
  }
}

object FeatureColumns {
  sealed trait Feature[T] {
    val label: String
    val prefix: String
  }

  sealed trait Single[T] extends Feature[T] {
    val label: String
    val value: T => Option[Double]
  }

  object Single {
    def fromLabel(label: String)(implicit sDateProvider: Long => SDateLike): Single[_] = label match {
      case BestPax.label => BestPax
    }
  }

  case object BestPax extends Single[Arrival] {
    override val label: String = "bestPax"
    override val prefix: String = "bestpax"
    override val value: Arrival => Option[Double] = (a: Arrival) => a.bestPcpPaxEstimate.pax.map(_.toDouble)
  }

  sealed trait OneToMany[T] extends Feature[T] {
    val label: String
    val value: T => Option[String]
  }

  object OneToMany {
    def fromLabel(label: String)(implicit sDateProvider: Long => SDateLike): OneToMany[_] = label match {
      case DayOfWeek.label => DayOfWeek()
      case PartOfDay.label => PartOfDay()
      case Carrier.label => Carrier
      case Origin.label => Origin
      case FlightNumber.label => FlightNumber
    }
  }

  case class DayOfWeek()(implicit sDateProvider: Long => SDateLike) extends OneToMany[Arrival] {
    override val label: String = DayOfWeek.label
    override val prefix: String = "dow"
    override val value: Arrival => Option[String] =
      (a: Arrival) => Option(sDateProvider(a.Scheduled).getDayOfWeek.toString)
  }

  object DayOfWeek {
    val label: String = "dayOfTheWeek"
  }

  case class PartOfDay()(implicit sDateProvider: Long => SDateLike) extends OneToMany[Arrival] {
    override val label: String = PartOfDay.label
    override val prefix: String = "pod"
    override val value: Arrival => Option[String] =
      (a: Arrival) => Option((sDateProvider(a.Scheduled).getHours / 12).toString)
  }

  object PartOfDay {
    val label: String = "partOfDay"
  }

  case object Carrier extends OneToMany[Arrival] {
    override val label: String = "carrier"
    override val prefix: String = "car"
    override val value: Arrival => Option[String] = (a: Arrival) => Option(a.flightCode.carrierCode.code)
  }

  case object Origin extends OneToMany[Arrival] {
    override val label: String = "origin"
    override val prefix: String = "ori"
    override val value: Arrival => Option[String] = (a: Arrival) => Option(a.Origin.iata)
  }

  case object FlightNumber extends OneToMany[Arrival] {
    override val label: String = "flightNumber"
    override val prefix: String = "fln"
    override val value: Arrival => Option[String] = (a: Arrival) => Option(a.flightCode.voyageNumberLike.numeric.toString)
  }
}
