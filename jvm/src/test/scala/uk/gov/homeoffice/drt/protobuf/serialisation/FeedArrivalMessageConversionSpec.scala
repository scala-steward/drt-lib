package uk.gov.homeoffice.drt.protobuf.serialisation

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.FeedArrivalGenerator
import uk.gov.homeoffice.drt.protobuf.messages.FeedArrivalsMessage.{ForecastFeedArrivalsDiffMessage, LiveFeedArrivalsDiffMessage}
import uk.gov.homeoffice.drt.protobuf.serialisation.FeedArrivalMessageConversion.{forecastArrivalToMessage, liveArrivalToMessage}
import uk.gov.homeoffice.drt.protobuf.serialisation.FlightMessageConversion.uniqueArrivalToMessage

class FeedArrivalMessageConversionSpec extends AnyWordSpec with Matchers {
  "FeedArrivalMessageConversion" should {
    "serialise and deserialise forecast state to a snapshot and back without loss" in {
      val fa1 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 1)
      val fa2 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 2)
      val state = Map(
        fa1.unique -> fa1,
        fa2.unique -> fa2,
      )

      val snapshotMessage = FeedArrivalMessageConversion.forecastStateToSnapshotMessage(state)
      val restored = FeedArrivalMessageConversion.forecastStateFromSnapshotMessage(snapshotMessage)

      restored should be(state)
    }

    "serialise and deserialise live state to a snapshot and back without loss" in {
      val fa1 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 1)
      val fa2 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 2)
      val state = Map(
        fa1.unique -> fa1,
        fa2.unique -> fa2,
      )

      val snapshotMessage = FeedArrivalMessageConversion.liveStateToSnapshotMessage(state)
      val restored = FeedArrivalMessageConversion.liveStateFromSnapshotMessage(snapshotMessage)

      restored should be(state)
    }

    "return updated state given existing state and a forecast feed arrivals diff message" in {
      val fa1 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 1)
      val fa2 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 2)
      val state = Map(
        fa1.unique -> fa1,
        fa2.unique -> fa2,
      )

      val fa3 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 3)
      val fa4 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 4)
      val removals = Seq(uniqueArrivalToMessage(fa1.unique))
      val updates = Seq(fa3, fa4).map(forecastArrivalToMessage)

      val msg = ForecastFeedArrivalsDiffMessage(Option(1L), removals, updates)
      val updated = FeedArrivalMessageConversion.forecastStateFromMessage(msg, state)

      updated should be(Map(
        fa2.unique -> fa2,
        fa3.unique -> fa3,
        fa4.unique -> fa4,
      ))
    }

    "return updated state given existing state and a live feed arrivals diff message" in {
      val fa1 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 1)
      val fa2 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 2)
      val state = Map(
        fa1.unique -> fa1,
        fa2.unique -> fa2,
      )

      val fa3 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 3)
      val fa4 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 4)
      val removals = Seq(uniqueArrivalToMessage(fa1.unique))
      val updates = Seq(fa3, fa4).map(liveArrivalToMessage)

      val msg = LiveFeedArrivalsDiffMessage(Option(1L), removals, updates)
      val updated = FeedArrivalMessageConversion.liveStateFromMessage(msg, state)

      updated should be(Map(
        fa2.unique -> fa2,
        fa3.unique -> fa3,
        fa4.unique -> fa4,
      ))
    }

    "return a forecast diff message given a sequence of arrivals and an empty state" in {
      val fa1 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 1)
      val fa2 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 2)
      val arrivals = Seq(fa1, fa2)

      val maybeMessage = FeedArrivalMessageConversion.forecastArrivalsToMaybeDiffMessage(() => 1L, processRemovals = true)
      val maybe = maybeMessage((arrivals, Map.empty))

      val expected = ForecastFeedArrivalsDiffMessage(Option(1L), Seq.empty, arrivals.map(forecastArrivalToMessage))

      maybe should be(Some(expected))
    }

    "return a forecast diff message given a sequence of arrivals and an existing state" in {
      val fa1 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 1)
      val fa2 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 2)
      val state = Map(
        fa1.unique -> fa1,
        fa2.unique -> fa2,
      )
      val fa3 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 3)
      val fa4 = FeedArrivalGenerator.forecast(operator = Some("BA"), voyageNumber = 4)
      val arrivals = Seq(fa3, fa4)

      val maybeMessage = FeedArrivalMessageConversion.forecastArrivalsToMaybeDiffMessage(() => 1L, processRemovals = true)
      val maybe = maybeMessage((arrivals, state))

      val expected = ForecastFeedArrivalsDiffMessage(Option(1L), Seq(uniqueArrivalToMessage(fa1.unique), uniqueArrivalToMessage(fa2.unique)), arrivals.map(forecastArrivalToMessage))

      maybe should be(Some(expected))
    }

    "return a live diff message given a sequence of arrivals and an empty state" in {
      val fa1 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 1)
      val fa2 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 2)
      val arrivals = Seq(fa1, fa2)

      val maybeMessage = FeedArrivalMessageConversion.liveArrivalsToMaybeDiffMessage(() => 1L, processRemovals = true)
      val maybe = maybeMessage((arrivals, Map.empty))

      val expected = LiveFeedArrivalsDiffMessage(Option(1L), Seq.empty, arrivals.map(liveArrivalToMessage))

      maybe should be(Some(expected))
    }

    "return a live diff message given a sequence of arrivals and an existing state" in {
      val fa1 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 1)
      val fa2 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 2)
      val state = Map(
        fa1.unique -> fa1,
        fa2.unique -> fa2,
      )
      val fa3 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 3)
      val fa4 = FeedArrivalGenerator.live(operator = Some("BA"), voyageNumber = 4)
      val arrivals = Seq(fa3, fa4)

      val maybeMessage = FeedArrivalMessageConversion.liveArrivalsToMaybeDiffMessage(() => 1L, processRemovals = true)
      val maybe = maybeMessage((arrivals, state))

      val expected = LiveFeedArrivalsDiffMessage(Option(1L), Seq(uniqueArrivalToMessage(fa1.unique), uniqueArrivalToMessage(fa2.unique)), arrivals.map(liveArrivalToMessage))

      maybe should be(Some(expected))
    }
  }

}
