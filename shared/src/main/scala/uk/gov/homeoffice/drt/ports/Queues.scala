package uk.gov.homeoffice.drt.ports

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.LocalDate
import upickle.default._

object Queues {

  sealed trait QueueStatus

  object QueueStatus {
    implicit val rw: ReadWriter[QueueStatus] = ReadWriter.merge(macroRW[Open.type], macroRW[Closed.type])
  }

  case object Open extends QueueStatus

  case object Closed extends QueueStatus

  case class QueueFallbacks(queues: (LocalDate, Terminal) => Seq[Queue]) {
    val fallbacks: PartialFunction[(Queue, PaxType), Seq[Queue]] = {
      case (EGate, _: EeaPaxType) => Seq(EeaDesk, QueueDesk, NonEeaDesk)
      case (EGate, _: NonEeaPaxType) => Seq(NonEeaDesk, QueueDesk, EeaDesk)
      case (EeaDesk, _: PaxType) => Seq(NonEeaDesk, QueueDesk)
      case (NonEeaDesk, _: PaxType) => Seq(EeaDesk, QueueDesk)
      case (_, _) => Seq()
    }

    def availableFallbacks(terminal: Terminal): (Queue, PaxType, LocalDate) => Seq[Queue] =
      (queue, paxType, date) => {
        val availableQueues = queues(date, terminal)
        fallbacks((queue, paxType)).filter(availableQueues.contains)
      }
  }

  sealed trait Queue extends ClassNameForToString with Ordered[Queue] {
    override def compare(that: Queue): Int = toString.compareTo(that.toString)

    val stringValue: String
  }

  object Queue {
    implicit val rw: ReadWriter[Queue] = ReadWriter.merge(
      macroRW[InvalidQueue.type],
      macroRW[EeaDesk.type],
      macroRW[EGate.type],
      macroRW[NonEeaDesk.type],
      macroRW[FastTrack.type],
      macroRW[Transfer.type],
      macroRW[QueueDesk.type],
    )

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
    override val stringValue: String = "invalidqueue"
  }

  case object EeaDesk extends Queue {
    override val stringValue: String = "eeadesk"
  }

  case object EGate extends Queue {
    override val stringValue: String = "egate"
  }

  case object NonEeaDesk extends Queue {
    override val stringValue: String = "noneeadesk"
  }

  case object FastTrack extends Queue {
    override val stringValue: String = "fasttrack"
  }

  case object Transfer extends Queue {
    override val stringValue: String = "transfer"
  }

  case object QueueDesk extends Queue {
    override val stringValue: String = "queuedesk"
  }

  val queueOrder: List[Queue] = List(QueueDesk, EeaDesk, EGate, NonEeaDesk, FastTrack)

  def inOrder(queuesToSort: Seq[Queue]): Seq[Queue] = queueOrder.filter(queuesToSort.contains)

  val displayName: Function[Queue, String] = {
    case EeaDesk => "EEA"
    case NonEeaDesk => "Non-EEA"
    case EGate => "e-Gates"
    case FastTrack => "Fast Track"
    case Transfer => "Transfer"
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
