package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSource
import uk.gov.homeoffice.drt.ports.{ApiPaxTypeAndQueueCount, Queues}
import upickle.default.{ReadWriter, macroRW}

case class Splits(splits: Set[ApiPaxTypeAndQueueCount],
                  source: SplitSource,
                  maybeEventType: Option[EventType],
                  splitStyle: SplitStyle = PaxNumbers) {
  lazy val totalExcludingTransferPax: Double = Splits.totalExcludingTransferPax(splits)
  lazy val totalPax: Double = Splits.totalPax(splits)
}

object Splits {
  def totalExcludingTransferPax(splits: Set[ApiPaxTypeAndQueueCount]): Double = splits.filter(s => s.queueType != Queues.Transfer).toList.map(_.paxCount).sum

  def totalPax(splits: Set[ApiPaxTypeAndQueueCount]): Double = splits.toList.map(s => {
    s.paxCount
  }).sum

  implicit val rw: ReadWriter[Splits] = macroRW
}
