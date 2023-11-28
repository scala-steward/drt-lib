package uk.gov.homeoffice.drt.time

import scala.util.{Success, Try}

trait DateLike extends Ordered[DateLike] {
  val timeZone: String

  val year: Int
  val month: Int
  val day: Int
  val toISOString: String = f"$year-$month%02d-$day%02d"
  val ddmmyyyy: String = f"$day%02d/$month%02d/$year"

  override def toString: String = toISOString

  override def compare(that: DateLike): Int =
    if (toISOString < that.toISOString) -1
    else if (toISOString > that.toISOString) 1
    else 0

  def to(endDate: LocalDate)(implicit toSDateLike: DateLike => SDateLike): Seq[LocalDate] = {
    val start = if (this < endDate) this else endDate
    val end = if (this < endDate) endDate else this
    LazyList
      .iterate(toSDateLike(start))(_.addDays(1))
      .takeWhile(_.toLocalDate <= end)
      .map(_.toLocalDate)
  }

}

object DateLike {
  def parse[A <: DateLike](toDateLike: (Int, Int, Int) => A): String => Option[A] =
    (dateString: String) => Try(
      dateString
        .split("-")
        .take(3)
        .toList
        .map(_.toInt)
    ) match {
      case Success(year :: month :: day :: _) =>
        Option(toDateLike(year, month, day))
      case _ => None
    }
}

object DateLikeOrdering extends Ordering[DateLike] {
  override def compare(x: DateLike, y: DateLike): Int = x.compare(y)
}
