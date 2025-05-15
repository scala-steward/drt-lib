package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.models.CrunchMinute
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.EeaDesk
import uk.gov.homeoffice.drt.ports.Terminals.T2

class QueueSlotSerialiserTest extends AnyWordSpec with Matchers {
  "QueueSlotSerialiser" should {
    "convert to and from a row" in {
      val crunchMinute = CrunchMinute(
        terminal = T2,
        queue = EeaDesk,
        minute = 1L,
        paxLoad = 100,
        workLoad = 99,
        deskRec = 98,
        waitTime = 97,
        maybePaxInQueue = Option(100),
        deployedDesks = Option(101),
        deployedWait = Option(102),
        maybeDeployedPaxInQueue = Option(103),
        lastUpdated = Option(100L)
      )
      val row = QueueSlotSerialiser.toRow(PortCode("LHR"))(crunchMinute, 15)
      val deserialised = QueueSlotSerialiser.fromRow(row)
      deserialised should be(crunchMinute)
    }
  }
}
