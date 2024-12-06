package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.tables.{BorderCrossing, BorderCrossingRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

class BorderCrossingSerialiserTest extends AnyWordSpec with Matchers {
  "BorderCrossingSerialiser" should {
    "convert to and from a row" in {
      val passengersHourly = BorderCrossing(PortCode("LHR"), Terminal("T1"), UtcDate(2020, 1, 1), 1, 100)
      val updatedAt = 123L
      val row = BorderCrossingSerialiser.toRow(passengersHourly, updatedAt)
      val expectedRow = BorderCrossingRow("LHR", "T1", "2020-01-01", 1, 100, new Timestamp(updatedAt))
      row should be(expectedRow)
      val fromRow = BorderCrossingSerialiser.fromRow(row)
      fromRow should be(passengersHourly)
    }
  }
}
