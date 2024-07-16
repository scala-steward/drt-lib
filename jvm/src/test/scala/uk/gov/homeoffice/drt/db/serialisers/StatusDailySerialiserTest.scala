package uk.gov.homeoffice.drt.db.serialisers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.{StatusDaily, StatusDailyRow}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.LocalDate

import java.sql.Timestamp

class StatusDailySerialiserTest extends AnyWordSpec with Matchers {
  "StatusDailySerialiser" should {
    "convert to and from a row" in {
      val statusDaily = StatusDaily(PortCode("LHR"), Terminal("T1"), LocalDate(2020, 1, 1), Option(1L), Option(2L), Option(3L), Option(4L))
      val row = StatusDailySerialiser.toRow(statusDaily)
      val expectedRow = StatusDailyRow("LHR", "T1", "2020-01-01", Option(new Timestamp(1L)), Option(new Timestamp(2L)), Option(new Timestamp(3L)), Option(new Timestamp(4L)))
      row should be(expectedRow)
      val fromRow = StatusDailySerialiser.fromRow(row)
      fromRow should be(statusDaily)
    }
  }
}
