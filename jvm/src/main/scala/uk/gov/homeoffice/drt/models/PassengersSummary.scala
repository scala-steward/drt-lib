package uk.gov.homeoffice.drt.models

import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.time.LocalDate

case class PassengersSummaries(summaries: Iterable[PassengersSummary]) {
  def ++(other: PassengersSummaries): PassengersSummaries = PassengersSummaries(summaries ++ other.summaries)
  def ++(other: Seq[PassengersSummary]): PassengersSummaries = PassengersSummaries(summaries ++ other)
}

object PassengersSummaries {
  def empty: PassengersSummaries = PassengersSummaries(Seq())
}

case class PassengersSummary(regionName: String,
                             portCode: String,
                             terminalName: Option[String],
                             totalCapacity: Int,
                             drtQueueCounts: Map[Queue, Int],
                             bxQueueCounts: Map[Queue, Int],
                             maybeDate: Option[LocalDate],
                             maybeHour: Option[Int],
                            )

