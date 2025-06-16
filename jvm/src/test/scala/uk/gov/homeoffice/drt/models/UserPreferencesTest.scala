package uk.gov.homeoffice.drt.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.models.UserPreferences.deserializeMap

class UserPreferencesTest extends AnyWordSpec with Matchers {
  "deserializeMap" should {
    "return empty map when data is None" in {
      val result = deserializeMap(None, _.toInt)
      result shouldEqual Map.empty[String, Int]
    }

    "return empty map when data is empty" in {
      val result = deserializeMap(Some(""), _.toInt)
      result shouldEqual Map.empty[String, Int]
    }

    "parse valid JSON data to Ints correctly" in {
      val json = "bhx:30;lhr:60"
      val result = deserializeMap(Some(json), _.toInt)
      result shouldEqual Map("bhx" -> 30, "lhr" -> 60)
    }

    "throw an exception for invalid JSON data" in {
      val json = """invalid"""
      an[IllegalArgumentException] should be thrownBy deserializeMap(Some(json), _.toInt)
    }

    "parse valid JSON data to Sets correctly" in {
      val json = "lhr:T2,T3,T5;lgw:N"
      val result = deserializeMap(Some(json), _.split(",").toSet)
      result shouldEqual Map("lhr" -> Set("T2", "T3", "T5"), "lgw" -> Set("N"))
    }
  }

  "UserPreferences.deserializeMap" should {
    "deserialize 'lhr:30;bhx:60' to Map[String, Int]" in {
      val input = "lhr:30;bhx:60"
      val expected = Map("lhr" -> 30, "bhx" -> 60)
      val result = UserPreferences.deserializeMap(Some(input), _.toInt)
      result shouldEqual expected
    }
  }

  "UserPreferences" should {
    "serialize and deserialize portDashboardIntervalMinutes correctly" in {
      val input = Map("port1" -> 10, "port2" -> 20)
      val serialized = UserPreferences.serializeMap(input, (value: Int) => value.toString)
      val deserialized = UserPreferences.deserializeMap(Option(serialized), _.toInt)
      deserialized shouldEqual input
    }

    "serialize and deserialize portDashboardTerminals correctly" in {
      val input = Map("lhr" -> Set("t2", "t3"), "bhx" -> Set("t2"))
      val serialized = UserPreferences.serializeMap(input, (values: Set[String]) => values.mkString(","))
      val deserialized = UserPreferences.deserializeMap(Option(serialized), _.split(",").toSet)
      deserialized shouldEqual input
    }
  }
}
