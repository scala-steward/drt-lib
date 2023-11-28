package uk.gov.homeoffice.drt.ports.config.slas

import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.updates.ConfigUpdate

case class SlasUpdate(effectiveFrom: Long,
                      configItem: Map[Queue, Int],
                      maybeOriginalEffectiveFrom: Option[Long]
                     ) extends ConfigUpdate[Map[Queue, Int]]

object SlasUpdate {
  implicit val rw: upickle.default.ReadWriter[SlasUpdate] = upickle.default.macroRW
}
