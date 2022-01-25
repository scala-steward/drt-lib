package uk.gov.homeoffice.drt.arrivals

import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import upickle.default.{ReadWriter, macroRW}

sealed trait UniqueArrivalLike {
  val number: Int
  val terminal: Terminal
  val scheduled: Long
}

trait WithTerminal[A] extends Ordered[A] {
  def terminal: Terminal
}

trait WithTimeAccessor {
  def timeValue: Long
}

case class LegacyUniqueArrival(number: Int, terminal: Terminal, scheduled: Long) extends UniqueArrivalLike

object LegacyUniqueArrival {
  def apply(number: Int,
            terminalName: String,
            scheduled: Long): LegacyUniqueArrival = LegacyUniqueArrival(number, Terminal(terminalName), scheduled)
}

case class UniqueArrival(number: Int, terminal: Terminal, scheduled: Long, origin: PortCode)
  extends WithTimeAccessor
    with WithTerminal[UniqueArrival]
    with UniqueArrivalLike {

  lazy val legacyUniqueArrival: LegacyUniqueArrival = LegacyUniqueArrival(number, terminal, scheduled)

  override def compare(that: UniqueArrival): Int =
    scheduled.compare(that.scheduled) match {
      case 0 => terminal.compare(that.terminal) match {
        case 0 => number.compare(that.number) match {
          case 0 => origin.iata.compare(that.origin.iata)
          case c => c
        }
        case c => c
      }
      case c => c
    }

  override def timeValue: Long = scheduled

  def legacyUniqueId: Int = s"$terminal$scheduled$number".hashCode

  val equalWithinScheduledWindow: (UniqueArrival, Int) => Boolean = (searchKey, windowMillis) =>
    searchKey.number == this.number && searchKey.terminal == this.terminal && Math.abs(searchKey.scheduled - this.scheduled) <= windowMillis

  def equalsLegacy(lua: LegacyUniqueArrival): Boolean =
    lua.number == number && lua.scheduled == scheduled && lua.terminal == terminal
}

object UniqueArrival {
  implicit val rw: ReadWriter[UniqueArrival] = macroRW

  def apply(arrival: Arrival): UniqueArrival = UniqueArrival(arrival.VoyageNumber.numeric, arrival.Terminal, arrival.Scheduled, arrival.Origin)

  def apply(number: Int,
            terminalName: String,
            scheduled: Long,
            origin: String): UniqueArrival = UniqueArrival(number, Terminal(terminalName), scheduled, PortCode(origin))

  def atTime: Long => UniqueArrival = (time: Long) => UniqueArrival(0, "", time, "")
}
