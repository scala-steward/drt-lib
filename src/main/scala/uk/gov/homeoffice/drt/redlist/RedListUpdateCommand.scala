package uk.gov.homeoffice.drt.redlist

import upickle.default.{ReadWriter, macroRW}

sealed trait RedListUpdateCommand

case class SetRedListUpdate(originalDate: Long, redListUpdate: RedListUpdate) extends RedListUpdateCommand

object SetRedListUpdate {
  implicit val rw: ReadWriter[SetRedListUpdate] = macroRW
}

case class DeleteRedListUpdates(millis: Long) extends RedListUpdateCommand
