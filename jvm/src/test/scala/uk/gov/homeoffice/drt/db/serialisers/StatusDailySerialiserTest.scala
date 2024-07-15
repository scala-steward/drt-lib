package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.{StatusDaily, StatusDailyRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.UtcDate

import java.sql.Timestamp

class StatusDailySerialiserTest extends AnyWordSpec with Matchers {
  "StatusDailySerialiser" should {
    "convert to and from a row" in {
      val statusDaily = StatusDaily(PortCode("LHR"), Terminal("T1"), UtcDate(2020, 1, 1), 1L, 2L, 3L)
      val row = StatusDailySerialiser.toRow(statusDaily)
      val expectedRow = StatusDailyRow("LHR", "T1", "2020-01-01", new Timestamp(1L), new Timestamp(2L), new Timestamp(3L))
      row should be(expectedRow)
      val fromRow = StatusDailySerialiser.fromRow(row)
      fromRow should be(statusDaily)
    }
  }
}
