package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.time.MilliTimes.oneMinuteMillis
import uk.gov.homeoffice.drt.time.{MilliTimes, SDateLike}
import upickle.default.{ReadWriter, macroRW}

import scala.collection.immutable.{List, NumericRange}
import scala.concurrent.duration.{DurationLong, FiniteDuration}
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

case class TotalPaxSource(pax: Option[Int], feedSource: FeedSource)

case class Arrival(Operator: Option[Operator],
                   CarrierCode: CarrierCode,
                   VoyageNumber: VoyageNumber,
                   FlightCodeSuffix: Option[FlightCodeSuffix],
                   Status: ArrivalStatus,
                   Estimated: Option[Long],
                   PredictedTouchdown: Option[Prediction[Long]],
                   Actual: Option[Long],
                   EstimatedChox: Option[Long],
                   ActualChox: Option[Long],
                   Gate: Option[String],
                   Stand: Option[String],
                   MaxPax: Option[Int],
                   ActPax: Option[Int],
                   TranPax: Option[Int],
                   RunwayID: Option[String],
                   BaggageReclaimId: Option[String],
                   AirportID: PortCode,
                   Terminal: Terminal,
                   Origin: PortCode,
                   Scheduled: Long,
                   PcpTime: Option[Long],
                   FeedSources: Set[FeedSource],
                   CarrierScheduled: Option[Long],
                   ApiPax: Option[Int],
                   ScheduledDeparture: Option[Long],
                   RedListPax: Option[Int],
                   TotalPax: Set[TotalPaxSource]
                  )
  extends WithUnique[UniqueArrival] with Updatable[Arrival] {
  lazy val differenceFromScheduled: Option[FiniteDuration] = Actual.map(a => (a - Scheduled).milliseconds)

  val paxOffPerMinute = 20

  def suffixString: String = FlightCodeSuffix match {
    case None => ""
    case Some(s) => s.suffix
  }

  def displayStatus: ArrivalStatus = {

    val fifteenMinutes = 15 * 60 * 1000

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

  def hasPcpDuring(start: SDateLike, end: SDateLike): Boolean = {
    val firstPcpMilli = PcpTime.getOrElse(0L)
    val lastPcpMilli = firstPcpMilli + millisToDisembark(ActPax.getOrElse(0), 20)
    val firstInRange = start.millisSinceEpoch <= firstPcpMilli && firstPcpMilli <= end.millisSinceEpoch
    val lastInRange = start.millisSinceEpoch <= lastPcpMilli && lastPcpMilli <= end.millisSinceEpoch
    firstInRange || lastInRange
  }

  def isRelevantToPeriod(rangeStart: SDateLike, rangeEnd: SDateLike): Boolean =
    Arrival.isRelevantToPeriod(rangeStart, rangeEnd)(this)

  def millisToDisembark(pax: Int, paxPerMinute: Int): Long = {
    val minutesToDisembark = (pax.toDouble / paxPerMinute).ceil
    val oneMinuteInMillis = 60 * 1000
    (minutesToDisembark * oneMinuteInMillis).toLong
  }

  val bestPcpPaxEstimate: TotalPaxSource = {
    val matchedTotalPax = TotalPax match {
      case totalPax if totalPax.exists(tp => tp.feedSource == ScenarioSimulationSource && tp.pax.isDefined) =>
        excludeTransferPax(totalPax.find(tp => tp.feedSource == ScenarioSimulationSource))
      case totalPax if totalPax.exists(tp => tp.feedSource == LiveFeedSource && tp.pax.isDefined) =>
        excludeTransferPax(totalPax.find(tp => tp.feedSource == LiveFeedSource && tp.pax.isDefined))
      case totalPax if totalPax.exists(tp => tp.feedSource == ApiFeedSource && tp.pax.isDefined) =>
        totalPax.find(tp => tp.feedSource == ApiFeedSource && tp.pax.isDefined)
      case totalPax if totalPax.exists(tp => tp.feedSource == ForecastFeedSource && tp.pax.isDefined) =>
        excludeTransferPax(totalPax.find(tp => tp.feedSource == ForecastFeedSource && tp.pax.isDefined))
      case totalPax if totalPax.exists(tp => tp.feedSource == HistoricApiFeedSource && tp.pax.isDefined) =>
        totalPax.find(tp => tp.feedSource == HistoricApiFeedSource && tp.pax.isDefined)
      case totalPax if totalPax.exists(tp => tp.feedSource == AclFeedSource && tp.pax.isDefined) =>
        excludeTransferPax(totalPax.find(tp => tp.feedSource == AclFeedSource))
      case _ if fallBackToFeedSource(ActPax).isDefined =>
        excludeTransferPax(fallBackToFeedSource(ActPax))
      case totalPax =>
        totalPax.find(tp => tp.feedSource == UnknownFeedSource)
    }
    matchedTotalPax.getOrElse(TotalPaxSource(None, UnknownFeedSource))
  }

  def fallBackToFeedSource(actPax: Option[Int]): Option[TotalPaxSource] = {
    FeedSources match {
      case feedSource if feedSource.contains(LiveFeedSource) =>
        Some(TotalPaxSource(actPax, LiveFeedSource))
      case feedSource if feedSource.contains(ForecastFeedSource) =>
        Some(TotalPaxSource(actPax, ForecastFeedSource))
      case feedSource if feedSource.contains(AclFeedSource) =>
        Some(TotalPaxSource(actPax, AclFeedSource))
      case _ =>
        None
    }
  }

  def excludeTransferPax(totalPaxSource: Option[TotalPaxSource]) = {
    val excludeTransPax = totalPaxSource.flatMap(_.pax.map(_ - TranPax.getOrElse(0)))
    if (excludeTransPax.exists(_ > 0)) {
      totalPaxSource.map(tps => tps.copy(pax = excludeTransPax))
    } else {
      totalPaxSource.map(tps => tps.copy(pax = Some(0)))
    }
  }

  def bestArrivalTime(timeToChox: Long, considerPredictions: Boolean): Long =
    (ActualChox, EstimatedChox, Actual, Estimated, PredictedTouchdown, Scheduled) match {
      case (Some(actChox), _, _, _, _, _) => actChox
      case (_, Some(estChox), _, _, _, _) => estChox
      case (_, _, Some(touchdown), _, _, _) => touchdown + timeToChox
      case (_, _, _, Some(estimated), _, _) => estimated + timeToChox
      case (_, _, _, _, Some(Prediction(_, predictedTd)), _) if considerPredictions => predictedTd + timeToChox
      case (_, _, _, _, _, scheduled) => scheduled + timeToChox
    }

  def walkTime(timeToChox: Long, firstPaxOff: Long, considerPredictions: Boolean): Option[Long] =
    PcpTime.map(pcpTime => pcpTime - (bestArrivalTime(timeToChox, considerPredictions) + firstPaxOff))

  def minutesOfPaxArrivals: Int = {
    val totalPax = bestPcpPaxEstimate
    if (totalPax.pax.getOrElse(0) <= 0) 0
    else (totalPax.pax.getOrElse(0).toDouble / paxOffPerMinute).ceil.toInt - 1
  }

  lazy val pcpRange: NumericRange[Long] = {
    val pcpStart = MilliTimes.timeToNearestMinute(PcpTime.getOrElse(0L))

    val pcpEnd = pcpStart + oneMinuteMillis * minutesOfPaxArrivals

    pcpStart to pcpEnd by oneMinuteMillis
  }

  def paxDeparturesByMinute(departRate: Int): Iterable[(Long, Int)] = {
    val totalPax = bestPcpPaxEstimate.pax.getOrElse(0)
    val maybeRemainingPax = totalPax % departRate match {
      case 0 => None
      case someLeftovers => Option(someLeftovers)
    }
    val paxByMinute = List.fill(totalPax / departRate)(departRate) ::: maybeRemainingPax.toList
    pcpRange.zip(paxByMinute)
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
}

object Arrival {
  val flightCodeRegex: Regex = "^([A-Z0-9]{2,3}?)([0-9]{1,4})([A-Z]*)$".r

  def isInRange(rangeStart: Long, rangeEnd: Long)(needle: Long): Boolean =
    rangeStart < needle && needle < rangeEnd

  def isRelevantToPeriod(rangeStart: SDateLike, rangeEnd: SDateLike)(arrival: Arrival): Boolean = {
    val rangeCheck: Long => Boolean = isInRange(rangeStart.millisSinceEpoch, rangeEnd.millisSinceEpoch)

    rangeCheck(arrival.Scheduled) ||
      rangeCheck(arrival.Estimated.getOrElse(0)) ||
      rangeCheck(arrival.EstimatedChox.getOrElse(0)) ||
      rangeCheck(arrival.Actual.getOrElse(0)) ||
      rangeCheck(arrival.ActualChox.getOrElse(0)) ||
      arrival.hasPcpDuring(rangeStart, rangeEnd)
  }

  def summaryString(arrival: Arrival): String = arrival.AirportID + "/" + arrival.Terminal + "@" + arrival.Scheduled + "!" + arrival.flightCodeString

  def standardiseFlightCode(flightCode: String): String = {
    val flightCodeRegex = "^([A-Z0-9]{2,3}?)([0-9]{1,4})([A-Z]?)$".r

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
  implicit val arrivalRw: ReadWriter[Arrival] = macroRW
  implicit val totalPaxSourceRw: ReadWriter[TotalPaxSource] = macroRW

  def apply(Operator: Option[Operator],
            Status: ArrivalStatus,
            Estimated: Option[Long],
            PredictedTouchdown: Option[Prediction[Long]],
            Actual: Option[Long],
            EstimatedChox: Option[Long],
            ActualChox: Option[Long],
            Gate: Option[String],
            Stand: Option[String],
            MaxPax: Option[Int],
            ActPax: Option[Int],
            TranPax: Option[Int],
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
            ApiPax: Option[Int] = None,
            ScheduledDeparture: Option[Long] = None,
            RedListPax: Option[Int] = None,
            TotalPax: Set[TotalPaxSource] = Set.empty
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
      PredictedTouchdown = PredictedTouchdown,
      Actual = Actual,
      EstimatedChox = EstimatedChox,
      ActualChox = ActualChox,
      Gate = Gate,
      Stand = Stand,
      MaxPax = MaxPax,
      ActPax = ActPax,
      TranPax = TranPax,
      RunwayID = RunwayID,
      BaggageReclaimId = BaggageReclaimId,
      AirportID = AirportID,
      Terminal = Terminal,
      Origin = Origin,
      Scheduled = Scheduled,
      PcpTime = PcpTime,
      FeedSources = FeedSources,
      CarrierScheduled = CarrierScheduled,
      ApiPax = ApiPax,
      ScheduledDeparture = ScheduledDeparture,
      RedListPax = RedListPax,
      TotalPax = TotalPax
    )
  }
}
