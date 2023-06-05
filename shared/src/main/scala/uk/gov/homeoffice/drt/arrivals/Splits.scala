package uk.gov.homeoffice.drt.arrivals

import ujson.Value.Value
import uk.gov.homeoffice.drt.arrivals.SplitStyle.PaxNumbers
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSource
import uk.gov.homeoffice.drt.ports.{ApiPaxTypeAndQueueCount, Queues}
import upickle.default.{ReadWriter, macroRW, read, readwriter, writeJs}

import scala.collection.mutable
import scala.util.Try

case class Splits(splits: Set[ApiPaxTypeAndQueueCount],
                  source: SplitSource,
                  maybeEventType: Option[EventType],
                  splitStyle: SplitStyle = PaxNumbers) {
  lazy val totalPax: Int = Math.round(Splits.totalPax(splits)).toInt
  lazy val transPax: Int = Math.round(Splits.transferPax(splits)).toInt
  lazy val totalExcludingTransferPax: Double = totalPax - transPax
}

object Splits {

  def transferPax(splits: Set[ApiPaxTypeAndQueueCount]): Double = splits.filter(s => s.queueType == Queues.Transfer).toList.map(_.paxCount).sum

  def totalPax(splits: Set[ApiPaxTypeAndQueueCount]): Double = splits.toList.map(s => {
    s.paxCount
  }).sum

  implicit val splitsReadWriter: ReadWriter[Splits] =
    readwriter[Value].bimap[Splits](
      (splits: Splits) => ujson.Obj(mutable.LinkedHashMap(
        "splits" -> Try(writeJs(splits.splits)).getOrElse(throw new Exception(s"Failed to write splits.splits: ${splits.splits}")),
        "source" -> Try(writeJs(splits.source)).getOrElse(throw new Exception(s"Failed to write splits.source: ${splits.source}")),
        "maybeEventType" -> Try(writeJs(splits.maybeEventType)).getOrElse(throw new Exception(s"Failed to write splits.maybeEventType: ${splits.maybeEventType}")),
        "splitStyle" -> Try(writeJs(splits.splitStyle)).getOrElse(throw new Exception(s"Failed to write splits.splitStyle: ${splits.splitStyle}")),
        )
      ),
      json => {
        val splits = read[Set[ApiPaxTypeAndQueueCount]](json("splits"))
        val source = read[SplitSource](json("source"))
        val maybeEventType = read[Option[EventType]](json("maybeEventType"))
        val splitStyle = read[SplitStyle](json("splitStyle"))
        Splits(splits, source, maybeEventType, splitStyle)
      }
    )
}
