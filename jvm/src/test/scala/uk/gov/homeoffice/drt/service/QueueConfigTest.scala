package uk.gov.homeoffice.drt.service

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk, NonEeaDesk, QueueDesk}
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2, Terminal}
import uk.gov.homeoffice.drt.time.LocalDate

import scala.collection.SortedMap

class QueueConfigTest extends AnyWordSpec with Matchers {
  private val config: SortedMap[LocalDate, Map[Terminal, Seq[Queues.Queue]]] = SortedMap(
    LocalDate(2014, 1, 1) -> Map(T1 -> Seq(EeaDesk, EGate, NonEeaDesk)),
    LocalDate(2025, 6, 2) -> Map(T1 -> Seq(QueueDesk)),
  )

  private val configProvider = QueueConfig.queuesForDateAndTerminal(config)

  "queuesForDateAndTerminal" should {
    val terminal = T1

    "return the queues configured in the most recent past config prior to 2023-10-01: 2014-01-01" in {
      configProvider(LocalDate(2023, 10, 1), terminal) shouldEqual Seq(EeaDesk, EGate, NonEeaDesk)
    }
    "return the queues configured in the most recent past prior to 2025-06-10: 2025-06-02" in {
      configProvider(LocalDate(2025, 6, 10), terminal) shouldEqual Seq(QueueDesk)
    }
  }

  "queuesForDateRangeAndTerminal" should {
    val terminal = T1
    "return the all queues configures for the given terminal when the date range spans only one config (1)" in {
      val queuesProvider = QueueConfig.queuesForDateRangeAndTerminal(config)

      val queues = queuesProvider(LocalDate(2023, 1, 1), LocalDate(2025, 6, 1), terminal)
      queues shouldEqual Set(EeaDesk, EGate, NonEeaDesk)
    }
    "return the all queues configures for the given terminal when the date range spans only one config (2)" in {
      val queuesProvider = QueueConfig.queuesForDateRangeAndTerminal(config)

      val queues = queuesProvider(LocalDate(2025, 6, 2), LocalDate(2025, 6, 10), terminal)
      queues shouldEqual Set(QueueDesk)
    }
    "return the all queues configures for the given terminal when the date range spans multiple configs" in {
      val queuesProvider = QueueConfig.queuesForDateRangeAndTerminal(config)

      val queues = queuesProvider(LocalDate(2014, 1, 1), LocalDate(2025, 6, 10), terminal)
      queues shouldEqual Set(EeaDesk, EGate, NonEeaDesk, QueueDesk)
    }
  }

  "allTerminalsIncludingHistoric" should {
    "return all terminals that have been configured in the past" in {
      val allTerminals = QueueConfig.allTerminalsIncludingHistoric(config ++ SortedMap(LocalDate(2017, 1, 1) -> Map(T1 -> Seq.empty, T2 -> Seq.empty)))
      allTerminals shouldEqual Seq(T1, T2)
    }
  }

  "terminalsForDate" should {
    "return the terminals configured in the most recent past config prior to 2023-10-01: 2014-01-01" in {
      val terminalsProvider = QueueConfig.terminalsForDate(config)
      terminalsProvider(LocalDate(2023, 10, 1)) shouldEqual Seq(T1)
    }
    "return the terminals configured in the most recent past prior to 2025-06-10: 2025-06-02" in {
      val terminalsProvider = QueueConfig.terminalsForDate(config ++ SortedMap(LocalDate(2025, 7, 1) -> Map(T1 -> Seq.empty, T2 -> Seq.empty)))
      terminalsProvider(LocalDate(2025, 8, 1)) shouldEqual Seq(T1, T2)
    }
  }

  "terminalsForDateRange" should {
    "return the terminals configured in the most recent past config prior to 2023-10-01: 2014-01-01" in {
      val terminalsProvider = QueueConfig.terminalsForDateRange(config)
      terminalsProvider(LocalDate(2023, 10, 1), LocalDate(2023, 10, 2)) shouldEqual Seq(T1)
    }
    "return the terminals configured in the most recent past prior to 2025-06-10: 2025-06-02" in {
      val terminalsProvider = QueueConfig.terminalsForDateRange(config ++ SortedMap(LocalDate(2025, 7, 1) -> Map(T2 -> Seq.empty)))
      terminalsProvider(LocalDate(2025, 6, 10), LocalDate(2025, 8, 2)) shouldEqual Seq(T1, T2)
    }
  }
}
