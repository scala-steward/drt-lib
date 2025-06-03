package uk.gov.homeoffice.drt.service

import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.LocalDate

import scala.collection.SortedMap

object QueueConfig {
  def queuesForDateAndTerminal(configOverTime: SortedMap[LocalDate, Map[Terminal, Seq[Queue]]]): (LocalDate, Terminal) => Seq[Queue] =
    (queryDate: LocalDate, terminal: Terminal) => {
      val relevantDates: SortedMap[LocalDate, Map[Terminal, Seq[Queue]]] = SortedMap.empty[LocalDate, Map[Terminal, Seq[Queue]]] ++ configOverTime.filter {
        case (configDate, _) => configDate <= queryDate
      }

      val queues = relevantDates.toSeq.reverse.headOption
        .map {
          case (_, terminalQueues) => terminalQueues.getOrElse(terminal, Seq.empty[Queue])
        }
        .getOrElse(Seq.empty[Queue])

      Queues.queueOrder.filter(queues.contains)
    }

  def terminalsForDate(configOverTime: SortedMap[LocalDate, Map[Terminal, Seq[Queue]]]): LocalDate => Seq[Terminal] =
    queryDate => mostRecentConfig(configOverTime, queryDate).keys.toSeq.sorted


  private def mostRecentConfig(configOverTime: SortedMap[LocalDate, Map[Terminal, Seq[Queue]]],
                               queryDate: LocalDate
                              ): Map[Terminal, Seq[Queue]] =
    maybeMostRecentDate(configOverTime.keys, queryDate) match {
      case Some(maxDate) => configOverTime(maxDate)
      case None => throw new NoSuchElementException(s"No config found for date $queryDate")
    }

  private def maybeMostRecentDate(dates: Iterable[LocalDate], queryDate: LocalDate): Option[LocalDate] =
    dates.filter(_ <= queryDate).toSeq.sorted.maxOption

  def terminalsForDateRange(configOverTime: SortedMap[LocalDate, Map[Terminal, Seq[Queue]]]): (LocalDate, LocalDate) => Seq[Terminal] =
    (start: LocalDate, end: LocalDate) => {
      val firstConfigDate = maybeMostRecentDate(configOverTime.keys.filter(_ <= start), start)
      val lastConfigDate = maybeMostRecentDate(configOverTime.keys.filter(_ <= end), end)

      val maybeTerminals = for {
        firstDate <- firstConfigDate
        lastDate <- lastConfigDate
      } yield {
        configOverTime
          .filter {
            case (d, _) => firstDate <= d && d <= lastDate
          }
          .foldLeft(Set.empty[Terminal]) {
            case (agg, (_, config)) => agg ++ config.keys
          }
          .toSeq.sorted
      }

      maybeTerminals.getOrElse(Seq.empty)
    }

  def allTerminalsIncludingHistoric(configOverTime: SortedMap[LocalDate, Map[Terminal, Seq[Queue]]]): Seq[Terminal] =
    configOverTime.values.flatMap(_.keys).toSet.toSeq.sorted

  def queuesForDateRangeAndTerminal(configOverTime: SortedMap[LocalDate, Map[Terminal, Seq[Queue]]]): (LocalDate, LocalDate, Terminal) => Seq[Queue] =
    (start: LocalDate, end: LocalDate, terminal: Terminal) => {
      val firstConfigDate = maybeMostRecentDate(configOverTime.keys.filter(_ <= start), start)
      val lastConfigDate = maybeMostRecentDate(configOverTime.keys.filter(_ <= end), end)

      val maybeQueues = for {
        firstDate <- firstConfigDate
        lastDate <- lastConfigDate
      } yield {
        val queues = configOverTime
          .filter {
            case (d, _) => firstDate <= d && d <= lastDate
          }
          .foldLeft(Set.empty[Queue]) {
            case (agg, (_, config)) => agg ++ config.getOrElse(terminal, Seq.empty)
          }
        Queues.queueOrder.filter(queues.contains)
      }

      maybeQueues.getOrElse(Seq.empty)
    }
}
