package uk.gov.homeoffice.drt.model

import uk.gov.homeoffice.drt.arrivals.WithLastUpdated
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import upickle.default._

case class CrunchMinute(terminal: Terminal,
                        queue: Queue,
                        minute: Long,
                        paxLoad: Double,
                        workLoad: Double,
                        deskRec: Int,
                        waitTime: Int,
                        maybePaxInQueue: Option[Int],
                        deployedDesks: Option[Int] = None,
                        deployedWait: Option[Int] = None,
                        maybeDeployedPaxInQueue: Option[Int] = None,
                        actDesks: Option[Int] = None,
                        actWait: Option[Int] = None,
                        lastUpdated: Option[Long] = None)
  extends MinuteLike[CrunchMinute, TQM]
    with WithMinute with WithLastUpdated {
  def equals(candidate: CrunchMinute): Boolean = this.copy(lastUpdated = None) == candidate.copy(lastUpdated = None)

  override def maybeUpdated(existing: CrunchMinute, now: Long): Option[CrunchMinute] =
    if (!equals(existing)) Option(copy(lastUpdated = Option(now)))
    else None

  lazy val key: TQM = TQM(terminal, queue, minute)

  override def toUpdatedMinute(now: Long): CrunchMinute = this.copy(lastUpdated = Option(now))

  override val toMinute: CrunchMinute = this

  def prettyPrint(implicit niceDate: Long => String): String = {
    s"CrunchMinute($terminal, $queue, ${niceDate(minute)}, $paxLoad pax, $workLoad work, $deskRec desks, $waitTime waits, $deployedDesks dep desks, $deployedWait dep wait, $actDesks act desks, $actWait act wait, ${lastUpdated.map(niceDate)} updated)"
  }
}

object CrunchMinute {
  implicit val rw: ReadWriter[CrunchMinute] = macroRW
}
