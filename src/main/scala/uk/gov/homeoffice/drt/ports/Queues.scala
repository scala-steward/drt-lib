package uk.gov.homeoffice.drt.ports

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import upickle.default._

object Queues {

  sealed trait QueueStatus

  object QueueStatus {
    implicit val rw: ReadWriter[QueueStatus] = macroRW
  }

  case object Open extends QueueStatus

  case object Closed extends QueueStatus

  case class QueueFallbacks(queues: Map[Terminal, Seq[Queue]]) {
    val fallbacks: PartialFunction[(Queue, PaxType), Seq[Queue]] = {
      case (EGate, _: EeaPaxType) => Seq(EeaDesk, QueueDesk, NonEeaDesk)
      case (EGate, _: NonEeaPaxType) => Seq(NonEeaDesk, QueueDesk, EeaDesk)
      case (EeaDesk, _: PaxType) => Seq(EeaDesk, QueueDesk)
      case (NonEeaDesk, _: PaxType) => Seq(EeaDesk, QueueDesk)
      case (_, _) => Seq()
    }

    def availableFallbacks(terminal: Terminal, queue: Queue, paxType: PaxType): Iterable[Queue] = {
      val availableQueues: List[Queue] = queues.get(terminal).toList.flatten
      fallbacks((queue, paxType)).filter(availableQueues.contains)
    }
  }

  sealed trait Queue extends ClassNameForToString with Ordered[Queue] {
    override def compare(that: Queue): Int = toString.compareTo(that.toString)
  }

  object Queue {
    implicit val rw: ReadWriter[Queue] = macroRW

    def apply(queueName: String): Queue = queueName.toLowerCase match {
      case "eeadesk" => EeaDesk
      case "egate" => EGate
      case "noneeadesk" => NonEeaDesk
      case "fasttrack" => FastTrack
      case "transfer" => Transfer
      case "queuedesk" => QueueDesk
      case _ => InvalidQueue
    }
  }

  case object InvalidQueue extends Queue {
    override val toString: String = ""
  }

  case object EeaDesk extends Queue

  case object EGate extends Queue

  case object NonEeaDesk extends Queue

  case object FastTrack extends Queue

  case object Transfer extends Queue

  case object QueueDesk extends Queue

  val queueOrder = List(QueueDesk, EGate, EeaDesk, NonEeaDesk, FastTrack)

  def inOrder(queuesToSort: Seq[Queue]): Seq[Queue] = queueOrder.filter(q => queuesToSort.contains(q))

  val displayName: Function[Queue, String] = {
    case EeaDesk => "EEA"
    case NonEeaDesk => "Non-EEA"
    case EGate => "e-Gates"
    case FastTrack => "Fast Track"
    case Transfer => "Tx"
    case QueueDesk => "Desk"
    case _ => "Invalid"
  }

  val forecastExportQueueOrderSansFastTrack = List(EeaDesk, NonEeaDesk, EGate)
  val forecastExportQueueOrderWithFastTrack = List(EeaDesk, NonEeaDesk, EGate, FastTrack)

  val deskExportQueueOrderSansFastTrack = List(EeaDesk, EGate, NonEeaDesk)
  val deskExportQueueOrderWithFastTrack = List(EeaDesk, EGate, NonEeaDesk, FastTrack)
  val exportQueueDisplayNames: Map[Queue, String] = Map(
    EeaDesk -> "EEA",
    NonEeaDesk -> "NON-EEA",
    EGate -> "E-GATES",
    FastTrack -> "FAST TRACK"
  )
}
