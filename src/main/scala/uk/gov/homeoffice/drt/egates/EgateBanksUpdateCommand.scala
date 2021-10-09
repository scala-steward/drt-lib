package uk.gov.homeoffice.drt.egates

import upickle.default.{ReadWriter, macroRW}

sealed trait EgateBanksUpdateCommand

case class SetEgateBanksUpdate(originalDate: Long, egateBanksUpdate: EgateBanksUpdate) extends EgateBanksUpdateCommand

object SetEgateBanksUpdate {
  implicit val rw: ReadWriter[SetEgateBanksUpdate] = macroRW
}

case class DeleteEgateBanksUpdates(millis: Long) extends EgateBanksUpdateCommand
