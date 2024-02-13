package uk.gov.homeoffice.drt.jsonformats

import org.scalatest.wordspec.AnyWordSpec
import spray.json.enrichAny
import uk.gov.homeoffice.drt.jsonformats.LocalDateJsonFormat.JsonFormat
import uk.gov.homeoffice.drt.time.LocalDate

class LocalDateJsonFormatTest extends AnyWordSpec {
  "LocalDateJsonFormat" should {
    "serialise and deserialise a LocalDate without loss" in {
      val localDate = LocalDate(2020, 1, 1)
      val serialised = localDate.toJson
      val deserialised = serialised.convertTo[LocalDate]
      assert(deserialised == localDate)
    }
  }
}
