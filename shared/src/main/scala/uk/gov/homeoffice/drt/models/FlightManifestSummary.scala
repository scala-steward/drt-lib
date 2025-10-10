package uk.gov.homeoffice.drt.models

import uk.gov.homeoffice.drt.Nationality
import uk.gov.homeoffice.drt.ports.PaxType
import upickle.default._

import scala.collection.SortedMap

case class FlightManifestSummary(arrivalKey: ManifestKey,
                                 ageRanges: SortedMap[PaxAgeRange, Int],
                                 nationalities: Map[Nationality, Int],
                                 paxTypes: Map[PaxType, Int]
                                ) {
  lazy val passengerCount: Int = Seq(ageRanges.values.sum, nationalities.values.sum, paxTypes.values.sum).max
}

object FlightManifestSummary {
  implicit val rw: ReadWriter[FlightManifestSummary] = macroRW
}
