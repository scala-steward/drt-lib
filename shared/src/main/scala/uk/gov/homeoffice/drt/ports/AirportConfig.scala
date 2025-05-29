package uk.gov.homeoffice.drt.ports

import uk.gov.homeoffice.drt.auth.Roles.Role
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitRatios
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.LocalDate
import upickle.default._

import scala.collection.immutable.SortedMap

trait AirportConfigLike {
  val config: AirportConfig
}

case class PortCode(iata: String) extends Ordered[PortCode] {
  override def toString: String = iata

  override def compare(that: PortCode): Int = iata.compareTo(that.iata)

  def nonEmpty: Boolean = iata.nonEmpty

  lazy val isDomestic: Boolean = Ports.isDomestic(this)
  lazy val isCta: Boolean = Ports.isCta(this)
  lazy val isDomesticOrCta: Boolean = Ports.isDomesticOrCta(this)
}

object PortCode {
  implicit val rw: ReadWriter[PortCode] = macroRW
}

case class AirportConfig(portCode: PortCode,
                         portName: String,
                         queuesByTerminal: SortedMap[LocalDate, SortedMap[Terminal, Seq[Queue]]],
                         divertedQueues: Map[Queue, Queue] = Map(),
                         flexedQueues: Set[Queue] = Set(),
                         slaByQueue: Map[Queue, Int],
                         timeToChoxMillis: Long = 300000L,
                         firstPaxOffMillis: Long = 180000L,
                         defaultWalkTimeMillis: Map[Terminal, Long],
                         terminalPaxSplits: Map[Terminal, SplitRatios],
                         terminalProcessingTimes: Map[Terminal, Map[PaxTypeAndQueue, Double]],
                         minMaxDesksByTerminalQueue24Hrs: Map[Terminal, Map[Queue, (List[Int], List[Int])]],
                         fixedPointExamples: Seq[String] = Seq(),
                         hasActualDeskStats: Boolean = false,
                         eGateBankSizes: Map[Terminal, Iterable[Int]],
                         minutesToCrunch: Int = 1440,
                         crunchOffsetMinutes: Int = 0,
                         hasEstChox: Boolean = true,
                         forecastExportQueueOrder: List[Queue] = Queues.forecastExportQueueOrderSansFastTrack,
                         desksExportQueueOrder: List[Queue] = Queues.deskExportQueueOrderSansFastTrack,
                         contactEmail: Option[String] = None,
                         outOfHoursContactPhone: Option[String] = None,
                         nationalityBasedProcTimes: Map[String, Double] = ProcessingTimes.nationalityProcessingTimes,
                         role: Role,
                         terminalPaxTypeQueueAllocation: Map[Terminal, Map[PaxType, Seq[(Queue, Double)]]],
                         hasTransfer: Boolean = false,
                         maybeCiriumEstThresholdHours: Option[Int] = None,
                         maybeCiriumTaxiThresholdMinutes: Option[Int] = Option(20),
                         feedSources: Seq[FeedSource],
                         feedSourceMonitorExemptions: Seq[FeedSource] = Seq(),
                         desksByTerminal: Map[Terminal, Int],
                         queuePriority: List[Queue] = List(EeaDesk, NonEeaDesk, QueueDesk, FastTrack, EGate),
                         assumedAdultsPerChild: Double = 1.0,
                         useTimePredictions: Boolean = false,
                         noLivePortFeed: Boolean = false,
                         aclDisabled: Boolean = false,
                         idealStaffAsDefault: Boolean = false
                        ) {
//  def queuesByTerminalWithDiversions: Map[Terminal, Map[Queue, Queue]] = queuesByTerminal
//    .mapValues { queues =>
//      val diversionQueues = divertedQueues.values.toSet
//      val nonDivertedQueues = queues
//        .filterNot(q => diversionQueues.contains(q))
//        .map(q => (q, q))
//      (nonDivertedQueues ++ divertedQueues).toMap
//    }.view.toMap

  def assertValid(): Unit = {
    queuesByTerminal.foreach {
      case (date, config) => config.values.flatten.toSet
        .filterNot(_ == Transfer)
        .foreach {
          queue: Queue =>
            assert(slaByQueue.contains(queue), s"Missing sla: ${date.toISOString} $queue @ $portCode")
        }
    }
    queuesByTerminal.foreach {
      case (date, config) => config.foreach {
        case (terminal, tQueues) =>
          assert(minMaxDesksByTerminalQueue24Hrs.contains(terminal), s"Missing min/max desks: ${date.toISOString} $terminal @ $portCode")
          tQueues
            .filterNot(_ == Transfer)
            .foreach {
              tQueue =>
                assert(minMaxDesksByTerminalQueue24Hrs(terminal).contains(tQueue), s"Missing min/max desks: ${date.toISOString} $tQueue for terminal $terminal @ $portCode")
            }
      }
    }
    terminalProcessingTimes.foreach {
      case (terminal, procTimes) =>
        procTimes.foreach {
          case (paxTypeAndQueue, procTime) =>
            assert(procTime > 0.3, s"Processing time for $paxTypeAndQueue at $terminal is too low @ $portCode (${procTime * 60} <= 20s)")
            assert(procTime < 3, s"Processing time for $paxTypeAndQueue at $terminal is too high @ $portCode (${procTime * 60} >= 180s)")
        }
    }
  }

  def minDesksByTerminalAndQueue24Hrs: Map[Terminal, Map[Queue, IndexedSeq[Int]]] = minMaxDesksByTerminalQueue24Hrs.mapValues(_.mapValues(_._1.toIndexedSeq).view.toMap).view.toMap

  def maxDesksByTerminalAndQueue24Hrs: Map[Terminal, Map[Queue, IndexedSeq[Int]]] = minMaxDesksByTerminalQueue24Hrs.mapValues(_.mapValues(_._2.toIndexedSeq).view.toMap).view.toMap

  def minDesksForTerminal24Hrs(tn: Terminal): Map[Queue, IndexedSeq[Int]] = minMaxDesksByTerminalQueue24Hrs.getOrElse(tn, Map()).mapValues(_._1.toIndexedSeq).view.toMap

  def maxDesksForTerminal24Hrs(tn: Terminal): Map[Queue, IndexedSeq[Int]] = minMaxDesksByTerminalQueue24Hrs.getOrElse(tn, Map()).mapValues(_._2.toIndexedSeq).view.toMap

  val terminals: Iterable[Terminal] = queuesByTerminal.headOption.map(_._2.keys).getOrElse(Iterable.empty)

  val terminalSplitQueueTypes: Map[Terminal, Set[Queue]] = terminalPaxSplits.map {
    case (terminal, splitRatios) =>
      (terminal, splitRatios.splits.map(_.paxType.queueType).toSet)
  }

  def queueTypeSplitOrder(terminal: Terminal): List[Queue] = Queues.queueOrder.filter {
    q =>
      terminalSplitQueueTypes.getOrElse(terminal, Set()).contains(q)
  }

//  def nonTransferQueues(terminalName: Terminal): Seq[Queue] = queuesByTerminal(terminalName).collect {
//    case queue if queue != Queues.Transfer => queue
//  }
}

object AirportConfig {
  implicit val rwQueues: ReadWriter[SortedMap[Terminal, Seq[Queue]]] = readwriter[Map[Terminal, Seq[Queue]]].bimap[SortedMap[Terminal, Seq[Queue]]](
    sm => Map[Terminal, Seq[Queue]]() ++ sm,
    m => SortedMap[Terminal, Seq[Queue]]() ++ m
  )

  implicit val rw: ReadWriter[AirportConfig] = macroRW
}
