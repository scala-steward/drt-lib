package uk.gov.homeoffice.drt.prediction.arrival

import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.prediction.ModelAndFeatures
import uk.gov.homeoffice.drt.time.SDateLike

//trait ArrivalModelAndFeatures extends ModelAndFeatures {
//  private def dayOfWeek(ts: Long)(implicit sDateProvider: Long => SDateLike): String = s"dow_${sDateProvider(ts).getDayOfWeek()}"
//  private def amPm(ts: Long)(implicit sDateProvider: Long => SDateLike): String = s"pod_${sDateProvider(ts).getHours() / 12}"
//
//  def prediction(arrival: Arrival)(implicit sDateProvider: Long => SDateLike): Option[Int] = {
//    val dowIdx = features.oneToManyValues.indexOf(dayOfWeek(arrival.Scheduled))
//    val partOfDayIds = features.oneToManyValues.indexOf(amPm(arrival.Scheduled))
//    for {
//      dowCo <- model.coefficients.toIndexedSeq.lift(dowIdx)
//      partOfDayCo <- model.coefficients.toIndexedSeq.lift(partOfDayIds)
//    } yield {
//      (model.intercept + dowCo + partOfDayCo).toInt
//    }
//  }
//}

trait ArrivalModelAndFeatures extends ModelAndFeatures {
  def prediction(arrival: Arrival, featureValues: Iterable[Arrival => String]): Option[Int] = {
    val coefficients = featureValues.map { featureValue =>
      val featureIdx = features.oneToManyValues.indexOf(featureValue(arrival))
      model.coefficients.toIndexedSeq.lift(featureIdx)
    }
    if (coefficients.forall(_.isDefined)) {
      Some((model.intercept + coefficients.map(_.get).sum).round.toInt)
    } else {
      None
    }
  }
}

object FeatureColumns {
  sealed trait SingleFeatureColumn[T] {
    val label: String
    val value: T => Option[Double]
  }

  object SingleFeatureColumn {
    def fromLabel(label: String)(implicit sDateProvider: Long => SDateLike): SingleFeatureColumn[_] = label match {
      case BestPax.label => BestPax
    }
  }

  case object BestPax extends SingleFeatureColumn[Arrival] {
    override val label: String = "bestPax"
    override val value: Arrival => Option[Double] = (a: Arrival) => a.bestPcpPaxEstimate.pax.map(_.toDouble)
  }

  sealed trait OneToManyFeatureColumn[T] {
    val label: String
    val value: T => Option[String]
  }

  object OneToManyFeatureColumn {
    def fromLabel(label: String)(implicit sDateProvider: Long => SDateLike): OneToManyFeatureColumn[_] = label match {
      case DayOfWeek.label => DayOfWeek()
      case PartOfDay.label => PartOfDay()
      case Carrier.label => Carrier
      case Origin.label => Origin
      case FlightNumber.label => FlightNumber
    }
  }

  case class DayOfWeek()(implicit sDateProvider: Long => SDateLike) extends OneToManyFeatureColumn[Arrival] {
    override val label: String = DayOfWeek.label
    override val value: Arrival => Option[String] =
      (a: Arrival) => Option(sDateProvider(a.Scheduled).getDayOfWeek().toString)
  }
  object DayOfWeek {
    val label: String = "dayOfTheWeek"
  }
  case class PartOfDay()(implicit sDateProvider: Long => SDateLike) extends OneToManyFeatureColumn[Arrival] {
    override val label: String = PartOfDay.label
    override val value: Arrival => Option[String] =
      (a: Arrival) => Option((sDateProvider(a.Scheduled).getHours() / 12).toString)
  }

  object PartOfDay {
    val label: String = "partOfDay"
  }
  case object Carrier extends OneToManyFeatureColumn[Arrival] {
    override val label: String = "carrier"
    override val value: Arrival => Option[String] = (a: Arrival) => Option(a.flightCode.carrierCode.code)
  }

  case object Origin extends OneToManyFeatureColumn[Arrival] {
    override val label: String = "origin"
    override val value: Arrival => Option[String] = (a: Arrival) => Option(a.Origin.iata)
  }

  case object FlightNumber extends OneToManyFeatureColumn[Arrival] {
    override val label: String = "flightNumber"
    override val value: Arrival => Option[String] = (a: Arrival) => Option(a.flightCode.voyageNumberLike.numeric.toString)
  }
}
