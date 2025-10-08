package uk.gov.homeoffice.drt.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AgeRangeTest extends AnyWordSpec with Matchers {
  "parse" should {
    "return UnknowAage given an unknown age string" in {
      val result = PaxAgeRange.parse(UnknownAge.title)

      result should ===(UnknownAge)
    }
  }

  "AgeRange" should {
    "serialise and deserialise without loss" in {
      val ageRange = AgeRange(25, Option(49))
      val json = upickle.default.write(ageRange)
      val ageRangeDeserialised = upickle.default.read[AgeRange](json)
      ageRangeDeserialised should ===(ageRange)
    }
  }
}
