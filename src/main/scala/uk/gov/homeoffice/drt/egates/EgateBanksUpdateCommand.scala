package uk.gov.homeoffice.drt.egates

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import upickle.default.{ReadWriter, macroRW}

sealed trait EgateBanksUpdateCommand

case class SetEgateBanksUpdate(terminal: Terminal, originalDate: Long, egateBanksUpdate: EgateBanksUpdate) extends EgateBanksUpdateCommand {
  lazy val firstMinuteAffected: Long =
    if (egateBanksUpdate.effectiveFrom < originalDate)
      egateBanksUpdate.effectiveFrom
    else originalDate
}

object SetEgateBanksUpdate {
  implicit val rw: ReadWriter[SetEgateBanksUpdate] = macroRW
}

case class DeleteEgateBanksUpdates(terminal: Terminal, millis: Long) extends EgateBanksUpdateCommand
