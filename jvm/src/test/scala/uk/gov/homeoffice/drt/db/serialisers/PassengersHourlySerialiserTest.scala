package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.tables.{PassengersHourly, PassengersHourlyRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

class PassengersHourlySerialiserTest extends AnyWordSpec with Matchers {
  "PassengersHourlySerialiser" should {
    "convert to and from a row" in {
      val passengersHourly = PassengersHourly(PortCode("LHR"), Terminal("T1"), Queue("EeaDesk"), UtcDate(2020, 1, 1), 1, 100)
      val updatedAt = 123L
      val row = PassengersHourlySerialiser.toRow(passengersHourly, updatedAt)
      val expectedRow = PassengersHourlyRow("LHR", "T1", "EeaDesk", "2020-01-01", 1, 100, new Timestamp(updatedAt))
      row should be(expectedRow)
      val fromRow = PassengersHourlySerialiser.fromRow(row)
      fromRow should be(passengersHourly)
    }
  }
}
