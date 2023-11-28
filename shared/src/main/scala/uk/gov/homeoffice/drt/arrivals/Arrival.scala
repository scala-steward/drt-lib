package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.prediction.arrival.{OffScheduleModelAndFeatures, ToChoxModelAndFeatures}
import uk.gov.homeoffice.drt.time.MilliTimes.oneMinuteMillis
import uk.gov.homeoffice.drt.time.{MilliTimes, SDateLike}
import upickle.default.{ReadWriter, macroRW}

import scala.collection.immutable.{List, NumericRange}
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.Try
import scala.util.matching.Regex


trait WithUnique[I] {
  def unique: I
}

trait Updatable[I] {
  def update(incoming: I): I
}

case class Prediction[A](updatedAt: Long, value: A)

object Prediction {
  implicit val predictionLong: ReadWriter[Prediction[Long]] = macroRW
  implicit val predictionInt: ReadWriter[Prediction[Int]] = macroRW
}

case class Passengers(actual: Option[Int], transit: Option[Int]) {

  def diffInActualAndTrans: Option[Int] = actual.map(_ - transit.getOrElse(0))

  def getPcpPax: Option[Int] = {
    diffInActualAndTrans match {
      case Some(a) if a > 0 => Option(a)
      case Some(a) if a <= 0 => Option(0)
      case _ => None
    }
  }
}

case class PaxSource(feedSource: FeedSource, passengers: Passengers) {

  def getPcpPax: Option[Int] = passengers.getPcpPax

}

case class Predictions(lastChecked: Long, predictions: Map[String, Int])

case class Arrival(Operator: Option[Operator],
                   CarrierCode: CarrierCode,
                   VoyageNumber: VoyageNumber,
                   FlightCodeSuffix: Option[FlightCodeSuffix],
                   Status: ArrivalStatus,
                   Estimated: Option[Long],
                   Predictions: Predictions,
                   Actual: Option[Long],
                   EstimatedChox: Option[Long],
                   ActualChox: Option[Long],
                   Gate: Option[String],
                   Stand: Option[String],
                   MaxPax: Option[Int],
                   RunwayID: Option[String],
                   BaggageReclaimId: Option[String],
                   AirportID: PortCode,
                   Terminal: Terminal,
                   Origin: PortCode,
                   Scheduled: Long,
                   PcpTime: Option[Long],
                   FeedSources: Set[FeedSource],
                   CarrierScheduled: Option[Long],
                   ScheduledDeparture: Option[Long],
                   RedListPax: Option[Int],
                   PassengerSources: Map[FeedSource, Passengers]
                  )
  extends WithUnique[UniqueArrival] with Updatable[Arrival] {
  lazy val differenceFromScheduled: Option[FiniteDuration] = Actual.map(a => (a - Scheduled).milliseconds)

  val paxOffPerMinute = 20
  val fifteenMinutes = 15 * 60 * 1000

  def suffixString: String = FlightCodeSuffix match {
    case None => ""
    case Some(s) => s.suffix
  }

  def displayStatus: ArrivalStatus = {
    (this.Estimated, this.ActualChox, this.Actual) match {
      case (_, _, _) if isCancelledStatus(this.Status.description.toLowerCase) => ArrivalStatus("Cancelled")
      case (_, _, _) if isDivertedStatus(this.Status.description.toLowerCase) => ArrivalStatus("Diverted")
      case (_, Some(_), _) => ArrivalStatus("On Chocks")
      case (_, _, Some(_)) => ArrivalStatus("Landed")
      case (Some(e), _, _) if this.Scheduled + fifteenMinutes < e => ArrivalStatus("Delayed")
      case (Some(_), _, _) => ArrivalStatus("Expected")
      case (None, _, _) => ArrivalStatus("Scheduled")

    }
  }

  def displayStatusMobile: ArrivalStatus = {
    (this.Estimated, this.ActualChox, this.Actual) match {
      case (_, _, _) if isCancelledStatus(this.Status.description.toLowerCase) => ArrivalStatus("Cnx")
      case (_, _, _) if isDivertedStatus(this.Status.description.toLowerCase) => ArrivalStatus("Dvt")
      case (_, Some(_), _) => ArrivalStatus("On Chk")
      case (_, _, Some(_)) => ArrivalStatus("Landed")
      case (Some(e), _, _) if this.Scheduled + fifteenMinutes < e => ArrivalStatus("Dla")
      case (Some(_), _, _) => ArrivalStatus("Exp")
      case (None, _, _) => ArrivalStatus("Sch")
    }

  }

  val isDivertedStatus: String => Boolean = description => description == "redirected" | description == "diverted"
  val isCancelledStatus: String => Boolean = description => description == "c" | description == "canceled" | description == "deleted / removed flight record" | description == "cancelled" | description.contains("deleted")

  val flightCode: FlightCode = FlightCode(CarrierCode, VoyageNumber, FlightCodeSuffix)

  def flightCodeString: String = flightCode.toString

  def withoutPcpTime: Arrival = copy(PcpTime = None)

  def isEqualTo(arrival: Arrival): Boolean =
    if (arrival.PcpTime.isDefined && PcpTime.isDefined)
      arrival == this
    else
      arrival.withoutPcpTime == withoutPcpTime

  lazy val uniqueId: Int = uniqueStr.hashCode
  lazy val uniqueStr: String = s"$Terminal$Scheduled${VoyageNumber.numeric}"

  def hasPcpDuring(start: SDateLike, end: SDateLike, sourceOrderPreference: List[FeedSource]): Boolean = {
    val firstPcpMilli = PcpTime.getOrElse(0L)
    val pcpPax = bestPaxEstimate(sourceOrderPreference).passengers.actual.getOrElse(0)
    val lastPcpMilli = firstPcpMilli + millisToDisembark(pcpPax, 20)
    val firstInRange = start.millisSinceEpoch <= firstPcpMilli && firstPcpMilli <= end.millisSinceEpoch
    val lastInRange = start.millisSinceEpoch <= lastPcpMilli && lastPcpMilli <= end.millisSinceEpoch
    firstInRange || lastInRange
  }

  def isRelevantToPeriod(rangeStart: SDateLike, rangeEnd: SDateLike, sourceOrderPreference: List[FeedSource]): Boolean =
    Arrival.isRelevantToPeriod(rangeStart, rangeEnd, sourceOrderPreference)(this)

  def millisToDisembark(pax: Int, paxPerMinute: Int): Long = {
    val minutesToDisembark = (pax.toDouble / paxPerMinute).ceil
    val oneMinuteInMillis = 60 * 1000
    (minutesToDisembark * oneMinuteInMillis).toLong
  }

  def bestPaxEstimate(sourceOrderPreference: Seq[FeedSource]): PaxSource =
    sourceOrderPreference
      .find(source => PassengerSources.get(source).exists(_.actual.isDefined))
      .flatMap(source => PassengerSources.get(source).map(PaxSource(source, _)))
      .getOrElse(PaxSource(UnknownFeedSource, Passengers(None, None)))

  def bestPcpPaxEstimate(sourceOrderPreference: Seq[FeedSource]): Option[Int] =
    if (isCancelled || Origin.isDomesticOrCta) Option(0) else bestPaxEstimate(sourceOrderPreference).getPcpPax

  lazy val predictedTouchdown: Option[Long] =
    Predictions.predictions
      .get(OffScheduleModelAndFeatures.targetName)
      .map(offScheduleMinutes => Scheduled + (offScheduleMinutes * oneMinuteMillis))

  lazy val minutesToChox: Int = Predictions.predictions.getOrElse(ToChoxModelAndFeatures.targetName, Arrival.defaultMinutesToChox)

  def bestArrivalTime(considerPredictions: Boolean): Long = {
    val millisToChox = minutesToChox * oneMinuteMillis
    (ActualChox, EstimatedChox, Actual, Estimated, predictedTouchdown, Scheduled) match {
      case (Some(actChox), _, _, _, _, _) => actChox
      case (_, Some(estChox), _, _, _, _) => estChox
      case (_, _, Some(touchdown), _, _, _) => touchdown + millisToChox
      case (_, _, _, Some(estimated), _, _) => estimated + millisToChox
      case (_, _, _, _, Some(predictedTd), _) if considerPredictions => predictedTd + millisToChox
      case (_, _, _, _, _, scheduled) => scheduled + millisToChox
    }
  }

  def walkTime(firstPaxOff: Long, considerPredictions: Boolean): Option[Long] =
    PcpTime.map(pcpTime => pcpTime - (bestArrivalTime(considerPredictions) + firstPaxOff))

  def minutesOfPaxArrivals(sourceOrderPreference: List[FeedSource]): Int = {
    val bestPax = bestPaxEstimate(sourceOrderPreference)
    if (bestPax.passengers.actual.getOrElse(0) <= 0) 0
    else (bestPax.passengers.actual.getOrElse(0).toDouble / paxOffPerMinute).ceil.toInt
  }

  def pcpRange(sourceOrderPreference: List[FeedSource]): NumericRange[Long] = {
    val pcpStart = MilliTimes.timeToNearestMinute(PcpTime.getOrElse(0L))

    val minutes = minutesOfPaxArrivals(sourceOrderPreference)
    val minutesToAdd = if (minutes > 0) minutes - 1 else 0
    val pcpEnd = pcpStart + minutesToAdd * oneMinuteMillis

    pcpStart to pcpEnd by oneMinuteMillis
  }

  def paxDeparturesByMinute(departRate: Int, sourceOrderPreference: List[FeedSource]): Iterable[(Long, Int)] = {
    val bestPax = bestPaxEstimate(sourceOrderPreference).passengers.actual.getOrElse(0)
    val maybeRemainingPax = bestPax % departRate match {
      case 0 => None
      case someLeftovers => Option(someLeftovers)
    }
    val paxByMinute = List.fill(bestPax / departRate)(departRate) ::: maybeRemainingPax.toList
    pcpRange(sourceOrderPreference).zip(paxByMinute)
  }

  lazy val unique: UniqueArrival = UniqueArrival(VoyageNumber.numeric, Terminal, Scheduled, Origin)

  def isCancelled: Boolean = Status.description match {
    case st if st.toLowerCase.contains("cancelled") => true
    case st if st.toLowerCase.contains("canceled") => true
    case st if st.toLowerCase.contains("deleted") => true
    case _ => false
  }

  override def update(incoming: Arrival): Arrival =
    incoming.copy(
      BaggageReclaimId = if (incoming.BaggageReclaimId.exists(_.nonEmpty)) incoming.BaggageReclaimId else this.BaggageReclaimId,
      Stand = if (incoming.Stand.exists(_.nonEmpty)) incoming.Stand else this.Stand,
      Gate = if (incoming.Gate.exists(_.nonEmpty)) incoming.Gate else this.Gate,
      RedListPax = if (incoming.RedListPax.nonEmpty) incoming.RedListPax else this.RedListPax
    )

  lazy val hasNoPaxSource: Boolean = !PassengerSources.values.exists(_.actual.nonEmpty)
}

object Arrival {
  val defaultMinutesToChox: Int = 5

  val flightCodeRegex: Regex = "^([A-Z0-9]{2,3}?)([0-9]{1,4})([A-Z]*)$".r

  def parseFlightNumber(code: String): Option[Int] = code match {
    case Arrival.flightCodeRegex(_, flightNumber, _) => Try(flightNumber.toInt).toOption
    case _ => None
  }

  def isInRange(rangeStart: Long, rangeEnd: Long)(needle: Long): Boolean =
    rangeStart <= needle && needle <= rangeEnd

  def isRelevantToPeriod(rangeStart: SDateLike, rangeEnd: SDateLike, sourceOrderPreference: List[FeedSource])(arrival: Arrival): Boolean = {
    val rangeCheck: Long => Boolean = isInRange(rangeStart.millisSinceEpoch, rangeEnd.millisSinceEpoch)

    rangeCheck(arrival.Scheduled) ||
      rangeCheck(arrival.Estimated.getOrElse(0)) ||
      rangeCheck(arrival.EstimatedChox.getOrElse(0)) ||
      rangeCheck(arrival.Actual.getOrElse(0)) ||
      rangeCheck(arrival.ActualChox.getOrElse(0)) ||
      arrival.hasPcpDuring(rangeStart, rangeEnd, sourceOrderPreference)
  }

  def summaryString(arrival: Arrival): String = arrival.AirportID + "/" + arrival.Terminal + "@" + arrival.Scheduled + "!" + arrival.flightCodeString

  def standardiseFlightCode(flightCode: String): String = {
    flightCode match {
      case flightCodeRegex(operator, flightNumber, suffix) =>
        val number = f"${flightNumber.toInt}%04d"
        f"$operator$number$suffix"
      case _ => flightCode
    }
  }

  implicit val arrivalStatusRw: ReadWriter[ArrivalStatus] = macroRW
  implicit val voyageNumberRw: ReadWriter[VoyageNumber] = macroRW
  implicit val arrivalSuffixRw: ReadWriter[FlightCodeSuffix] = macroRW
  implicit val operatorRw: ReadWriter[Operator] = macroRW
  implicit val portCodeRw: ReadWriter[PortCode] = macroRW
  implicit val predictionsRw: ReadWriter[Predictions] = macroRW
  implicit val arrivalRw: ReadWriter[Arrival] = macroRW
  implicit val totalPaxSourceRw: ReadWriter[PaxSource] = macroRW
  implicit val passengersSourceRw: ReadWriter[Passengers] = macroRW

  def apply(Operator: Option[Operator],
            Status: ArrivalStatus,
            Estimated: Option[Long],
            Predictions: Predictions,
            Actual: Option[Long],
            EstimatedChox: Option[Long],
            ActualChox: Option[Long],
            Gate: Option[String],
            Stand: Option[String],
            MaxPax: Option[Int],
            RunwayID: Option[String],
            BaggageReclaimId: Option[String],
            AirportID: PortCode,
            Terminal: Terminal,
            rawICAO: String,
            rawIATA: String,
            Origin: PortCode,
            Scheduled: Long,
            PcpTime: Option[Long],
            FeedSources: Set[FeedSource],
            CarrierScheduled: Option[Long] = None,
            ScheduledDeparture: Option[Long] = None,
            RedListPax: Option[Int] = None,
            PassengerSources: Map[FeedSource, Passengers] = Map.empty
           ): Arrival = {
    val (carrierCode: CarrierCode, voyageNumber: VoyageNumber, maybeSuffix: Option[FlightCodeSuffix]) = {
      val bestCode = (rawIATA, rawICAO) match {
        case (iata, _) if iata != "" => iata
        case (_, icao) if icao != "" => icao
        case _ => ""
      }

      FlightCode.flightCodeToParts(bestCode)
    }

    Arrival(
      Operator = Operator,
      CarrierCode = carrierCode,
      VoyageNumber = voyageNumber,
      FlightCodeSuffix = maybeSuffix,
      Status = Status,
      Estimated = Estimated,
      Predictions = Predictions,
      Actual = Actual,
      EstimatedChox = EstimatedChox,
      ActualChox = ActualChox,
      Gate = Gate,
      Stand = Stand,
      MaxPax = MaxPax,
      RunwayID = RunwayID,
      BaggageReclaimId = BaggageReclaimId,
      AirportID = AirportID,
      Terminal = Terminal,
      Origin = Origin,
      Scheduled = Scheduled,
      PcpTime = PcpTime,
      FeedSources = FeedSources,
      CarrierScheduled = CarrierScheduled,
      ScheduledDeparture = ScheduledDeparture,
      RedListPax = RedListPax,
      PassengerSources = PassengerSources
    )
  }
}
