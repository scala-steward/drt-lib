package uk.gov.homeoffice.drt.prediction.arrival.features

import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.ports.{ApiFeedSource, LiveFeedSource}
import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}

import scala.annotation.tailrec

object FeatureColumnsV2 {

  object Single {
    def fromLabel(label: String): SingleFeature[_] = label match {
      case BestPax.label => BestPax
    }
  }

  case object BestPax extends SingleFeature[Arrival] {
    override val label: String = "bestPax"
    override val prefix: String = "bestpax"
    override val value: Arrival => Option[Double] = _.bestPaxEstimate(List(ApiFeedSource, LiveFeedSource)).passengers.getPcpPax.map(_.toDouble)
  }

  object OneToMany {
    def fromLabel(label: String)
                 (implicit
                  sDateProvider: Long => SDateLike,
                  sDateFromLocalDate: LocalDate => SDateLike,
                 ): OneToManyFeature[_] = label match {
      case DayOfWeek.label => DayOfWeek()
      case WeekendDay.label => WeekendDay()
      case PartOfDay.label => PartOfDay()
      case Carrier.label => Carrier
      case Origin.label => Origin
      case FlightNumber.label => FlightNumber
      case MonthOfYear.label => MonthOfYear()
      case Year.label => Year()
      case DayOfMonth.label => DayOfMonth()
      case ChristmasDay.label => ChristmasDay()
      case Term1a.label => Term1a()
      case OctoberHalfTerm.label => OctoberHalfTerm()
      case Term1b.label => Term1b()
      case ChristmasHoliday.label => ChristmasHoliday()
      case Term2a.label => Term2a()
      case SpringHalfTerm.label => SpringHalfTerm()
      case Term2b.label => Term2b()
      case EasterHoliday.label => EasterHoliday()
      case Term3a.label => Term3a()
      case SummerHalfTerm.label => SummerHalfTerm()
      case Term3b.label => Term3b()
      case SummerHolidayScotland.label => SummerHolidayScotland()
      case SummerHoliday.label => SummerHoliday()
      case PostPandemicRecovery.label => PostPandemicRecovery(sDateFromLocalDate(LocalDate(2022, 6, 1)))
    }
  }

  case class MonthOfYear()(implicit sDateProvider: Long => SDateLike) extends OneToManyFeature[Arrival] {
    override val label: String = MonthOfYear.label
    override val prefix: String = "moy"
    override val value: Arrival => Option[String] =
      (a: Arrival) => Option(sDateProvider(a.Scheduled).getMonth.toString)
  }

  object MonthOfYear {
    val label: String = "monthOfYear"
  }

  object DayOfMonth {
    val label: String = "dayOfMonth"
  }

  case class DayOfMonth()(implicit sDateProvider: Long => SDateLike) extends OneToManyFeature[Arrival] {
    override val label: String = DayOfMonth.label
    override val prefix: String = "dom"
    override val value: Arrival => Option[String] =
      (a: Arrival) => Option(sDateProvider(a.Scheduled).getDate.toString)
  }

  object Year {
    val label: String = "year"
  }

  case class Year()(implicit sDateProvider: Long => SDateLike) extends OneToManyFeature[Arrival] {
    override val label: String = Year.label
    override val prefix: String = "year"
    override val value: Arrival => Option[String] =
      (a: Arrival) => Option(sDateProvider(a.Scheduled).getFullYear.toString)
  }

  case class DayOfWeek()(implicit sDateProvider: Long => SDateLike) extends OneToManyFeature[Arrival] {
    override val label: String = DayOfWeek.label
    override val prefix: String = "dow"
    override val value: Arrival => Option[String] =
      (a: Arrival) => Option(sDateProvider(a.Scheduled).getDayOfWeek.toString)
  }

  object DayOfWeek {
    val label: String = "dayOfTheWeek"
  }

  case class PostPandemicRecovery(recoveryDate: SDateLike)
                                 (implicit sDateProvider: Long => SDateLike) extends OneToManyFeature[Arrival] {
    override val label: String = PostPandemicRecovery.label
    override val prefix: String = "pdr"
    override val value: Arrival => Option[String] =
      (a: Arrival) => {
        val isPre = if (sDateProvider(a.Scheduled) < recoveryDate) "y" else "n"
        Option(isPre)
      }
  }

  object PostPandemicRecovery {
    val label: String = "postPanRecovery"
  }

  case class Term1a()
                   (implicit
                    val sDateTs: Long => SDateLike,
                    val sDateLocalDate: LocalDate => SDateLike,
                   ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = Term1a.label
    override val prefix: String = "term1a"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 9, 1), LocalDate(2022, 10, 21)),
      (LocalDate(2023, 9, 4), LocalDate(2023, 10, 20)),
      (LocalDate(2024, 9, 2), LocalDate(2024, 10, 25)),
      (LocalDate(2025, 9, 1), LocalDate(2025, 10, 24)),
      (LocalDate(2026, 9, 1), LocalDate(2026, 10, 23)),
    )
  }

  object Term1a {
    val label: String = "term1FirstHalf"
  }

  case class OctoberHalfTerm()
                            (implicit
                             val sDateTs: Long => SDateLike,
                             val sDateLocalDate: LocalDate => SDateLike,
                            ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = OctoberHalfTerm.label
    override val prefix: String = "octht"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 10, 22), LocalDate(2022, 10, 30)),
      (LocalDate(2023, 10, 21), LocalDate(2023, 10, 29)),
      (LocalDate(2024, 10, 26), LocalDate(2024, 11, 3)),
      (LocalDate(2025, 10, 25), LocalDate(2025, 11, 2)),
      (LocalDate(2026, 10, 24), LocalDate(2026, 11, 1)),
    )
  }

  object OctoberHalfTerm {
    val label: String = "octHalfTerm"
  }

  case class Term1b()
                   (implicit
                    val sDateTs: Long => SDateLike,
                    val sDateLocalDate: LocalDate => SDateLike,
                   ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = Term1b.label
    override val prefix: String = "term1b"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 10, 31), LocalDate(2022, 12, 18)),
      (LocalDate(2023, 10, 30), LocalDate(2023, 12, 21)),
      (LocalDate(2024, 11, 4), LocalDate(2024, 12, 20)),
      (LocalDate(2025, 11, 3), LocalDate(2025, 12, 19)),
      (LocalDate(2026, 11, 2), LocalDate(2026, 12, 18)),
    )
  }

  object Term1b {
    val label: String = "term1SecondHalf"
  }

  case class ChristmasHoliday()
                             (implicit
                              val sDateTs: Long => SDateLike,
                              val sDateLocalDate: LocalDate => SDateLike,
                             ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = ChristmasHoliday.label
    override val prefix: String = "xmashol"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 12, 19), LocalDate(2023, 1, 2)),
      (LocalDate(2023, 12, 22), LocalDate(2024, 1, 7)),
      (LocalDate(2024, 12, 21), LocalDate(2025, 1, 5)),
      (LocalDate(2025, 12, 20), LocalDate(2026, 1, 4)),
      (LocalDate(2026, 12, 19), LocalDate(2027, 1, 4)),
    )
  }

  object ChristmasHoliday {
    val label: String = "xmasHoliday"
  }

  case class ChristmasDay()(implicit sDateProvider: Long => SDateLike) extends OneToManyFeature[Arrival] {
    override val label: String = ChristmasDay.label
    override val prefix: String = "xmasday"
    override val value: Arrival => Option[String] =
      (a: Arrival) => {
        val date = sDateProvider(a.Scheduled).toLocalDate
        val xmas = date.month == 12 && date.day == 25
        Option(if (xmas) "1" else "no")
      }
  }

  object ChristmasDay {
    val label: String = "xmasday"
  }

  case class Term2a()
                   (implicit
                    val sDateTs: Long => SDateLike,
                    val sDateLocalDate: LocalDate => SDateLike,
                   ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = Term2a.label
    override val prefix: String = "term2a"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 1, 4), LocalDate(2022, 2, 13)),
      (LocalDate(2023, 1, 3), LocalDate(2023, 2, 12)),
      (LocalDate(2024, 1, 8), LocalDate(2024, 2, 11)),
      (LocalDate(2025, 1, 6), LocalDate(2025, 2, 14)),
      (LocalDate(2026, 1, 5), LocalDate(2026, 2, 13)),
      (LocalDate(2027, 1, 5), LocalDate(2027, 2, 12)),
    )
  }

  object Term2a {
    val label: String = "term2FirstHalf"
  }

  case class SpringHalfTerm()
                           (implicit
                            val sDateTs: Long => SDateLike,
                            val sDateLocalDate: LocalDate => SDateLike,
                           ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = SpringHalfTerm.label
    override val prefix: String = "sprht"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 2, 14), LocalDate(2022, 2, 20)),
      (LocalDate(2023, 2, 13), LocalDate(2023, 2, 19)),
      (LocalDate(2024, 2, 12), LocalDate(2024, 2, 18)),
      (LocalDate(2025, 2, 15), LocalDate(2025, 2, 23)),
      (LocalDate(2026, 2, 14), LocalDate(2026, 2, 22)),
      (LocalDate(2027, 2, 13), LocalDate(2027, 2, 21)),
    )
  }

  object SpringHalfTerm {
    val label: String = "springHalfTerm"
  }

  case class Term2b()
                   (implicit
                    val sDateTs: Long => SDateLike,
                    val sDateLocalDate: LocalDate => SDateLike,
                   ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = Term2b.label
    override val prefix: String = "term2b"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 2, 21), LocalDate(2022, 4, 3)),
      (LocalDate(2023, 2, 20), LocalDate(2023, 4, 2)),
      (LocalDate(2024, 2, 19), LocalDate(2024, 3, 28)),
      (LocalDate(2025, 2, 24), LocalDate(2025, 4, 4)),
      (LocalDate(2026, 2, 23), LocalDate(2026, 3, 27)),
      (LocalDate(2027, 2, 22), LocalDate(2027, 3, 25)),
    )
  }

  object Term2b {
    val label: String = "term2SecondHalf"
  }

  case class EasterHoliday()
                          (implicit
                           val sDateTs: Long => SDateLike,
                           val sDateLocalDate: LocalDate => SDateLike,
                          ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = EasterHoliday.label
    override val prefix: String = "easter"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 4, 4), LocalDate(2022, 4, 18)),
      (LocalDate(2023, 4, 3), LocalDate(2023, 4, 16)),
      (LocalDate(2024, 3, 29), LocalDate(2024, 4, 14)),
      (LocalDate(2025, 4, 5), LocalDate(2025, 4, 21)),
      (LocalDate(2026, 3, 28), LocalDate(2026, 4, 12)),
      (LocalDate(2027, 3, 26), LocalDate(2027, 4, 11)),
    )
  }

  object EasterHoliday {
    val label: String = "easterHoliday"
  }

  case class Term3a()
                   (implicit
                    val sDateTs: Long => SDateLike,
                    val sDateLocalDate: LocalDate => SDateLike,
                   ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = Term3a.label
    override val prefix: String = "term3a"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 4, 19), LocalDate(2022, 5, 29)),
      (LocalDate(2023, 4, 17), LocalDate(2023, 5, 28)),
      (LocalDate(2024, 4, 15), LocalDate(2024, 5, 26)),
      (LocalDate(2025, 4, 22), LocalDate(2025, 5, 23)),
      (LocalDate(2026, 4, 13), LocalDate(2026, 5, 22)),
      (LocalDate(2027, 4, 12), LocalDate(2027, 5, 28)),
    )
  }

  object Term3a {
    val label: String = "term3FirstHalf"
  }

  case class SummerHalfTerm()
                           (implicit
                            val sDateTs: Long => SDateLike,
                            val sDateLocalDate: LocalDate => SDateLike,
                           ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = SummerHalfTerm.label
    override val prefix: String = "sumht"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 5, 30), LocalDate(2022, 6, 5)),
      (LocalDate(2023, 5, 29), LocalDate(2023, 6, 4)),
      (LocalDate(2024, 5, 27), LocalDate(2024, 6, 2)),
      (LocalDate(2025, 5, 24), LocalDate(2025, 6, 1)),
      (LocalDate(2026, 5, 23), LocalDate(2026, 5, 31)),
      (LocalDate(2027, 5, 29), LocalDate(2027, 6, 6)),
    )
  }

  object SummerHalfTerm {
    val label: String = "summerHalfTerm"
  }

  case class Term3b()
                   (implicit
                    val sDateTs: Long => SDateLike,
                    val sDateLocalDate: LocalDate => SDateLike,
                   ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = Term3b.label
    override val prefix: String = "term3b"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 6, 6), LocalDate(2022, 7, 24)),
      (LocalDate(2023, 6, 5), LocalDate(2023, 7, 23)),
      (LocalDate(2024, 6, 3), LocalDate(2024, 7, 24)),
      (LocalDate(2025, 6, 2), LocalDate(2025, 7, 22)),
      (LocalDate(2026, 6, 1), LocalDate(2026, 7, 20)),
      (LocalDate(2027, 6, 7), LocalDate(2027, 7, 22)),
    )
  }

  object Term3b {
    val label: String = "term3SecondHalf"
  }

  case class SummerHolidayScotland()
                                  (implicit
                                   val sDateTs: Long => SDateLike,
                                   val sDateLocalDate: LocalDate => SDateLike,
                                  ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = SummerHolidayScotland.label
    override val prefix: String = "sumholsco"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 6, 28), LocalDate(2022, 8, 14)),
      (LocalDate(2023, 6, 26), LocalDate(2023, 8, 13)),
      (LocalDate(2024, 6, 24), LocalDate(2024, 8, 14)),
      (LocalDate(2025, 6, 25), LocalDate(2025, 8, 14)),
      (LocalDate(2026, 6, 25), LocalDate(2026, 8, 14)),
    )
  }

  object SummerHolidayScotland {
    val label: String = "summerHolidayScotland"
  }

  case class SummerHoliday()
                          (implicit
                           val sDateTs: Long => SDateLike,
                           val sDateLocalDate: LocalDate => SDateLike,
                          ) extends OneToManyFeature[Arrival] with HolidayLike {
    override val label: String = SummerHoliday.label
    override val prefix: String = "sumhol"
    override val dates: Seq[(LocalDate, LocalDate)] = Seq(
      (LocalDate(2022, 7, 25), LocalDate(2022, 8, 31)),
      (LocalDate(2023, 7, 24), LocalDate(2023, 9, 3)),
      (LocalDate(2024, 7, 25), LocalDate(2024, 9, 1)),
      (LocalDate(2025, 7, 23), LocalDate(2025, 8, 31)),
      (LocalDate(2025, 7, 21), LocalDate(2025, 8, 31)),
    )
  }

  object SummerHoliday {
    val label: String = "summerHoliday"
  }

  trait HolidayLike {
    val sDateTs: Long => SDateLike
    implicit val sDateLocalDate: LocalDate => SDateLike

    val dates: Seq[(LocalDate, LocalDate)]

    private lazy val ranges: Map[(LocalDate, LocalDate), Seq[LocalDate]] = dates.map {
      case (start, end) =>
        (start, end) -> localDateRange(start, end)
    }.toMap

    private lazy val baseValue: Int = 100 / ranges.values.map(_.size).min

    lazy val rangeValues: Map[(LocalDate, LocalDate), IndexedSeq[Int]] = ranges.map {
      case (startAndEnd, range) =>
        startAndEnd -> rangeBaseValues(range.length, baseValue)
    }

    def hasMissingDate()(implicit now: () => SDateLike) = {
      val today = now()
      val sixMonthsAgo = today.addMonths(-6).toLocalDate
      val inSixMonths = today.addMonths(6).toLocalDate
      val existsWithin6Months = dates.exists { case (start, end) =>
        (sixMonthsAgo <= start && start <= inSixMonths) || (sixMonthsAgo <= end && end <= inSixMonths)
      }
      !existsWithin6Months
    }

    val value: Arrival => Option[String] = (a: Arrival) => dayOfHoliday(sDateTs(a.Scheduled).toLocalDate)

    def localDateRange(start: LocalDate, end: LocalDate)
                      (implicit sdate: LocalDate => SDateLike): Seq[LocalDate] = {
      @tailrec
      def constructRange(date: LocalDate, acc: List[LocalDate]): List[LocalDate] = {
        if (date == end) (date :: acc).reverse
        else constructRange(sdate(date).addDays(1).toLocalDate, date :: acc)
      }

      constructRange(start, List.empty)
    }

    def dayOfHoliday(localDate: LocalDate): Option[String] = {
      val date = sDateLocalDate(localDate)
      val day = dates
        .find { case (start, end) =>
          val afterStart = date >= sDateLocalDate(start)
          val beforeEnd = date <= sDateLocalDate(end)
          afterStart && beforeEnd
        }
        .map { case (start, end) =>
          val daysInHoliday = ranges((start, end))
          val indexOfDate = daysInHoliday.indexOf(localDate)
          val value = rangeValues((start, end))(indexOfDate)
          value.toString
        }
        .getOrElse("no")
      Option(day)
    }

    def rangeBaseValues(length: Int, base: Int): IndexedSeq[Int] =
      (0 until length)
        .map(d => 100 * d / length)
        .map(pct => (pct - (pct % base)) / base)

  }

  case class WeekendDay()(implicit sDateProvider: Long => SDateLike) extends OneToManyFeature[Arrival] {
    override val label: String = WeekendDay.label
    override val prefix: String = "wkd"
    override val value: Arrival => Option[String] =
      (a: Arrival) => {
        val isWeekend = List(6, 7).contains(sDateProvider(a.Scheduled).getDayOfWeek)
        Option(if (isWeekend) "1" else "no")
      }
  }

  object WeekendDay {
    val label: String = "weekendDay"
  }

  case class PartOfDay()(implicit sDateProvider: Long => SDateLike) extends OneToManyFeature[Arrival] {
    override val label: String = PartOfDay.label
    override val prefix: String = "pod"
    override val value: Arrival => Option[String] =
      (a: Arrival) => Option((sDateProvider(a.Scheduled).getHours / 12).toString)
  }

  object PartOfDay {
    val label: String = "partOfDay"
  }

  case object Carrier extends OneToManyFeature[Arrival] {
    override val label: String = "carrier"
    override val prefix: String = "car"
    override val value: Arrival => Option[String] = (a: Arrival) => Option(a.flightCode.carrierCode.code)
  }

  case object Origin extends OneToManyFeature[Arrival] {
    override val label: String = "origin"
    override val prefix: String = "ori"
    override val value: Arrival => Option[String] = (a: Arrival) => Option(a.Origin.iata)
  }

  case object FlightNumber extends OneToManyFeature[Arrival] {
    override val label: String = "flightNumber"
    override val prefix: String = "fln"
    override val value: Arrival => Option[String] = (a: Arrival) => Option(a.flightCode.voyageNumberLike.numeric.toString)
  }
}
