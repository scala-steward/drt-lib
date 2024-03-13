package uk.gov.homeoffice.drt.protobuf.serialisation

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.actor.commands.{CrunchRequest, MergeArrivalsRequest}
import uk.gov.homeoffice.drt.time.{LocalDate, UtcDate}

class CrunchRequestMessageConversionTest extends AnyWordSpec with Matchers {
  "loadProcessingRequestToMessage" should {
    "Serialise and deserialise a CrunchRequest without losing anything" in {
      val request = CrunchRequest(LocalDate(2021, 1, 1), 1, 1)
      val result = CrunchRequestMessageConversion.loadProcessingRequestToMessage(request)
      val deserialised = CrunchRequestMessageConversion.loadProcessingRequestFromMessage(result)

      deserialised should ===(request)
    }
  }
  "mergeArrivalsRequestToMessage" should {
    "Serialise and deserialise a MergeArrival without losing anything" in {
      val request = MergeArrivalsRequest(UtcDate(2021, 1, 1))
      val result = CrunchRequestMessageConversion.mergeArrivalRequestToMessage(request)
      val deserialised = CrunchRequestMessageConversion.mergeArrivalsRequestFromMessage(result)

      deserialised should ===(request)
    }
  }
}
