package uk.gov.homeoffice.drt.splits

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.SplitStyle.{PaxNumbers, Percentage, Ratio}
import uk.gov.homeoffice.drt.arrivals.{ApiFlightWithSplits, Arrival, ArrivalGenerator, Passengers, SplitStyle, Splits}
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.{ApiSplitsWithHistoricalEGateAndFTPercentages, Historical, TerminalAverage}
import uk.gov.homeoffice.drt.ports.{ApiFeedSource, ApiPaxTypeAndQueueCount, LiveFeedSource, PaxTypeAndQueue, PaxTypes, Queues}
import uk.gov.homeoffice.drt.splits.ApiSplitsToSplitRatio.applyPaxSplitsToFlightPax

class ApiSplitsToSplitRatioSpec extends AnyWordSpec with Matchers {
  val paxFeedSourceOrder = List(ApiFeedSource, LiveFeedSource)

  "queueTotals" should {
    "return a map of queue to total pax given a map of PaxTypeAndQueue to total pax" in {
      val aggSplits = Map(
        PaxTypeAndQueue(PaxTypes.EeaMachineReadable, Queues.EeaDesk) -> 100,
        PaxTypeAndQueue(PaxTypes.EeaNonMachineReadable, Queues.EeaDesk) -> 100
      )

      val expected = Map(Queues.EeaDesk -> 200)
      val result = ApiSplitsToSplitRatio.queueTotals(aggSplits)

      assert(result == expected)
    }

    "correctly aggregate different pax types into the same queues" in {
      val aggSplits = Map(
        PaxTypeAndQueue(PaxTypes.EeaMachineReadable, Queues.EeaDesk) -> 100,
        PaxTypeAndQueue(PaxTypes.EeaMachineReadable, Queues.EGate) -> 100,
        PaxTypeAndQueue(PaxTypes.EeaNonMachineReadable, Queues.EeaDesk) -> 100
      )

      val expected = Map(Queues.EeaDesk -> 200, Queues.EGate -> 100)
      val result = ApiSplitsToSplitRatio.queueTotals(aggSplits)

      assert(result == expected)
    }
  }

  "applyPaxSplitsToFlightPax" should {
    "return 1 Pax Split of 1 EeaMachineReadable to Egate given 1 pax with a split of 1 EeaMachineReadable to Egate" when {
      "calculating the splits for each PaxType and Queue the the split should be applied as a ratio to flight pax" in {

        val splits = Splits(
          Set(ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 1, None, None)),
          TerminalAverage,
          None
        )

        val result = applyPaxSplitsToFlightPax(splits, 1)

        val expected = Splits(
          Set(ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 1, None, None)),
          TerminalAverage,
          None,
          SplitStyle("Ratio")
        )

        assert(result == expected)
      }
    }

    "return 1 Pax Split of 2 EeaMachineReadable to Egate given 2 pax with a split of 1 EeaMachineReadable to Egate then I should get " in {
      val splits = Splits(
        Set(ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 1, None, None)),
        TerminalAverage,
        None
      )

      val result = applyPaxSplitsToFlightPax(splits, 2)

      val expected = Splits(
        Set(ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 2, None, None)),
        TerminalAverage,
        None,
        SplitStyle("Ratio")
      )

      assert(result == expected)
    }

    "return pax splits of: 1 EeaMachineReadable to Egate and 1 EeaMachineReadable to Desk " +
      "Given 2 pax with a split of 1 EeaMachineReadable to Egate and 1 EeaMachineReadable to Desk" in {

      val splits = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 1, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 1, None, None)
        ),
        TerminalAverage,
        None
      )

      val result = applyPaxSplitsToFlightPax(splits, 2)

      val expected = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 1, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 1, None, None)
        ),
        TerminalAverage,
        None,
        SplitStyle("Ratio")
      )

      assert(result == expected)
    }

    "return pax totalling 3 Given 3 pax with a split of 1 EeaMachineReadable to Egate and 1 EeaMachineReadable to Desk" in {
      val splits = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 1, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 1, None, None)
        ),
        TerminalAverage,
        None
      )

      val result = applyPaxSplitsToFlightPax(splits, 3).splits.toList.map(_.paxCount).sum

      val expected = 3

      assert(result == expected)
    }

    "only contain splits with whole numbers given 3 pax with a split of 1 EeaMachineReadable to Egate and 1 EeaMachineReadable to Desk" in {
      val splits = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 1, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 1, None, None)
        ),
        TerminalAverage,
        None
      )

      val ratioSplits = applyPaxSplitsToFlightPax(splits, 3)

      val rounded = ratioSplits.splits.toList.map(_.paxCount.toInt).sum
      val notRounded = ratioSplits.splits.toList.map(_.paxCount).sum.toInt

      assert(rounded == notRounded)
    }

    "apply the correction to the largest queue" in {
      val splits = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 10, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 1, None, None)
        ),
        TerminalAverage,
        None
      )

      val result = applyPaxSplitsToFlightPax(splits, 12)

      val expected = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 11, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 1, None, None)
        ),
        TerminalAverage,
        None,
        SplitStyle("Ratio")
      )

      assert(expected == result)
    }

    "apply splits as a ratio to the pax total given a flight with all splits" in {
      val pax = 152
      val splits = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.NonEeaDesk, 11.399999999999999, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.FastTrack, 0.6, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 36.85000000000001, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.VisaNational, Queues.NonEeaDesk, 5.699999999999999, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 30.150000000000006, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaNonMachineReadable, Queues.EeaDesk, 15, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.VisaNational, Queues.FastTrack, 0.3, None, None)), Historical, None, Percentage)

      val expected = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.NonEeaDesk, 17, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.FastTrack, 1, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 56, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.VisaNational, Queues.NonEeaDesk, 9, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 46, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaNonMachineReadable, Queues.EeaDesk, 23, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.VisaNational, Queues.FastTrack, 0, None, None)), Historical, None, Ratio)

      val result = applyPaxSplitsToFlightPax(splits, pax)

      assert(result == expected)
    }

    "ignore the transfer queue given a a transfer split is included" in {
      val splits = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 10, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 1, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.Transfer, 5, None, None)
        ),
        TerminalAverage,
        None
      )

      val result = applyPaxSplitsToFlightPax(splits, 12)

      val expected = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 11, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 1, None, None)
        ),
        TerminalAverage,
        None,
        SplitStyle("Ratio")
      )

      assert(expected == result)
    }
  }
  "paxPerQueueUsingBestSplitsAsRatio" should {
    "return total pax broken down per queue given a flight with percentage splits" in {
      val flight = ArrivalGenerator
        .arrival(passengerSources = Map(LiveFeedSource -> Passengers(Option(152), None)))
      val splits = Splits(
        Set(
          ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.NonEeaDesk, 11.399999999999999, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.FastTrack, 0.6, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EGate, 36.85000000000001, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.VisaNational, Queues.NonEeaDesk, 5.699999999999999, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaMachineReadable, Queues.EeaDesk, 30.150000000000006, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.EeaNonMachineReadable, Queues.EeaDesk, 15, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.VisaNational, Queues.FastTrack, 0.3, None, None)), Historical, None, Percentage)

      val result: Option[Map[Queues.Queue, Int]] = ApiSplitsToSplitRatio.paxPerQueueUsingBestSplitsAsRatio(ApiFlightWithSplits(flight, Set(splits)), paxFeedSourceOrder)

      val expected: Option[Map[Queues.Queue, Int]] = Option(Map(
        Queues.EeaDesk -> 69,
        Queues.EGate -> 56,
        Queues.NonEeaDesk -> 26,
        Queues.FastTrack -> 1
      ))

      assert(result == expected)
    }

    "return the total broken down per queue given a flight with PaxNumbers splits" in {
      val flight = ArrivalGenerator
        .arrival(passengerSources = Map(LiveFeedSource -> Passengers(Option(100), None)))
      val splits = Splits(Set(
        ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.NonEeaDesk, 15, None, None),
        ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.FastTrack, 5, None, None)),
        Historical, None, PaxNumbers)

      val result = ApiSplitsToSplitRatio.paxPerQueueUsingBestSplitsAsRatio(ApiFlightWithSplits(flight, Set(splits)), paxFeedSourceOrder)

      val expected: Option[Map[Queues.Queue, Int]] = Option(Map(
        Queues.NonEeaDesk -> 75,
        Queues.FastTrack -> 25
      ))

      assert(result == expected)
    }

    "return the total broken down per queue given a flight with no pax number for live feed and splits ApiSplitsWithHistoricalEGateAndFTPercentages" in {
      val flight: Arrival = ArrivalGenerator
        .arrival(passengerSources = Map(), feedSources = Set(ApiFeedSource))
        .copy(PassengerSources = Map(ApiFeedSource -> Passengers(Some(100), None)))
      val splits = Splits(Set(
        ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.NonEeaDesk, 15, None, None),
        ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.FastTrack, 5, None, None)),
        ApiSplitsWithHistoricalEGateAndFTPercentages, None, PaxNumbers)

      val apiFlightWithSplits = ApiFlightWithSplits(flight, Set(splits))
      val bestSplits = apiFlightWithSplits.bestSplits
      assert(bestSplits.contains(Splits(
        Set(ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.NonEeaDesk, 15, None, None),
          ApiPaxTypeAndQueueCount(PaxTypes.NonVisaNational, Queues.FastTrack, 5, None, None)),
        ApiSplitsWithHistoricalEGateAndFTPercentages, None, PaxNumbers))
      )
      val result = ApiSplitsToSplitRatio.paxPerQueueUsingBestSplitsAsRatio(apiFlightWithSplits, paxFeedSourceOrder)

      val expected: Option[Map[Queues.Queue, Int]] = Option(Map(
        Queues.NonEeaDesk -> 75,
        Queues.FastTrack -> 25
      ))
      assert(result == expected)
    }
  }
}

