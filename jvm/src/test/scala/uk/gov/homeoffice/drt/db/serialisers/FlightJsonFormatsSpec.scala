package uk.gov.homeoffice.drt.db.serialisers

import spray.json._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.Predictions

class FlightJsonFormatsSpec extends AnyWordSpec with Matchers with FlightJsonFormats {
  "PredictionsJsonFormat" should {
    "deserialise the old field name lastChecked" in {
      val json = """{"lastChecked":1744732043025,"predictions":{"off-schedule":-10,"walk-time":125,"to-chox":7}}"""
      val expected = Predictions(1744732043025L, Map("off-schedule" -> -10, "walk-time" -> 125, "to-chox" -> 7))
      json.parseJson.convertTo[Predictions] shouldEqual expected
    }
    "deserialise the new field name lastUpdated" in {
      val json = """{"lastUpdated":1747211437085,"predictions":{"walk-time":563,"to-chox":8}}"""
      val expected = Predictions(1747211437085L, Map("walk-time" -> 563, "to-chox" -> 8))
      json.parseJson.convertTo[Predictions] shouldEqual expected
    }
    "serialise and deserialise without data loss" in {
      val predictions = Predictions(
        lastUpdated = 1747211437085L,
        predictions = Map("walk-time" -> 563, "to-chox" -> 8)
      )
      val json = predictions.toJson
      val deserialized = json.convertTo[Predictions]
      deserialized shouldEqual predictions
    }
  }
}
