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

    "compare correctly" in {
      val ageRange1 = AgeRange(0, Option(9))
      val ageRange2 = AgeRange(10, Option(17))
      val ageRange3 = AgeRange(18, Option(24))
      val ageRange4 = AgeRange(25, Option(49))
      val ageRange5 = AgeRange(50, Option(65))
      val ageRange6 = AgeRange(66, None)
      val unknownAge = UnknownAge

      val ageRanges: List[PaxAgeRange] = List(ageRange4, unknownAge, ageRange1, ageRange6, ageRange3, ageRange5, ageRange2)
      val sortedAgeRanges = ageRanges.sorted

      sortedAgeRanges should ===(List(ageRange1, ageRange2, ageRange3, ageRange4, ageRange5, ageRange6, unknownAge))
    }
  }
}
