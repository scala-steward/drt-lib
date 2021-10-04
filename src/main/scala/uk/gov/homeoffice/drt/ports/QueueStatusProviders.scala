package uk.gov.homeoffice.drt.ports

import uk.gov.homeoffice.drt.ports.Queues.{Closed, EGate, Open, Queue, QueueStatus}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import upickle.default._

object QueueStatusProviders {

  sealed trait QueueStatusProvider {
    def statusAt(terminal: Terminal, queue: Queue, hour: Int): QueueStatus
  }

  object QueueStatusProvider {
    implicit val rw: ReadWriter[QueueStatusProvider] = macroRW
  }

  case object QueuesAlwaysOpen extends QueueStatusProvider {
    implicit val rw: ReadWriter[QueueStatusProvider] = macroRW

    override def statusAt(terminal: Terminal, queue: Queue, hour: Int): QueueStatus = Open
  }

  case class HourlyStatuses(statusByTerminalQueueHour: Map[Terminal, Map[Queue, IndexedSeq[QueueStatus]]]) extends QueueStatusProvider {
    override def statusAt(terminal: Terminal, queue: Queue, hour: Int): QueueStatus =
      statusByTerminalQueueHour.get(terminal)
        .flatMap(_.get(queue).flatMap { statuses => statuses.lift(hour % 24) }).getOrElse(Closed)
  }

  object HourlyStatuses {
    implicit val rw: ReadWriter[HourlyStatuses] = macroRW
  }

  case class FlexibleEGatesForSimulation(eGateOpenHours: Seq[Int]) extends QueueStatusProvider {
    override def statusAt(t: Terminal, queue: Queue, hour: Int): QueueStatus =
      (queue, hour) match {
        case (EGate, hour) if !eGateOpenHours.contains(hour) => Closed
        case _ => Open
      }
  }

  object FlexibleEGatesForSimulation {
    implicit val rw: ReadWriter[FlexibleEGatesForSimulation] = macroRW
  }

}
