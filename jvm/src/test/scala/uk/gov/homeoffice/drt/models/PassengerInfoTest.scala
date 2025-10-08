package uk.gov.homeoffice.drt.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.models.PassengerInfo.ageRangesForDate
import uk.gov.homeoffice.drt.time.SDate

class PassengerInfoTest extends AnyWordSpec with Matchers {
  "Deserializing age ranges" should {
    "get back the correct age range " in {
      val ageRangeStrings = PassengerInfo.ageRangesForDate(Some(SDate(System.currentTimeMillis()))).map(_.title)

      val result = ageRangeStrings.map(PaxAgeRange.parse)

      result should ===(PassengerInfo.ageRangesForDate(Some(SDate(System.currentTimeMillis()))))
    }
  }
}
