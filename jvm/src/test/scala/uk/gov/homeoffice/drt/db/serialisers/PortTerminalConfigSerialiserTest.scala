package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.tables.{PortTerminalConfig, PortTerminalConfigRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

class PortTerminalConfigSerialiserTest extends AnyWordSpec with Matchers {
  "PortTerminalConfigSerialiser" should {
    "convert to and from a row" in {
      val config = PortTerminalConfig(PortCode("LHR"), Terminal("T1"), Option(10), 1L)
      val row = PortTerminalConfigSerialiser.toRow(config)
      val expectedRow = PortTerminalConfigRow("LHR", "T1", Option(10), new Timestamp(1L))
      row should be(expectedRow)
      val fromRow = PortTerminalConfigSerialiser.fromRow(row)
      fromRow should be(config)
    }
  }
}
