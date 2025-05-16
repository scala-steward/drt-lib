package uk.gov.homeoffice.drt.services.exports

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import uk.gov.homeoffice.drt.arrivals.{ApiFlightWithSplits, ArrivalExportHeadings, CodeShares}
import uk.gov.homeoffice.drt.models.{ManifestKey, VoyageManifest, VoyageManifests}
import uk.gov.homeoffice.drt.ports.FeedSource
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.services.exports.FlightExports.{apiIsInvalid, splitsForSources}
import uk.gov.homeoffice.drt.time.LocalDate


trait FlightsWithSplitsExport {
  def start: LocalDate

  def end: LocalDate

  def terminals: Seq[Terminal]

  val flightsFilter: (ApiFlightWithSplits, Seq[Terminal]) => Boolean

  val paxFeedSourceOrder: List[FeedSource]

  protected def flightWithSplitsToCsvRow(fws: ApiFlightWithSplits): List[String] =
    FlightExports.flightWithSplitsToCsvFields(paxFeedSourceOrder)(fws.apiFlight) ++
      List(apiIsInvalid(fws)) ++
      splitsForSources(fws, paxFeedSourceOrder)

  val headings: String = ArrivalExportHeadings.arrivalWithSplitsHeadings

  def rowValues(fws: ApiFlightWithSplits, maybeManifest: Option[VoyageManifest]): Seq[String] = flightWithSplitsToCsvRow(fws)

  val standardFilter: (ApiFlightWithSplits, Seq[Terminal]) => Boolean = (fws, terminals) => terminals.contains(fws.apiFlight.Terminal)

  lazy val uniqueArrivalsWithCodeShares: Seq[ApiFlightWithSplits] => Iterable[ApiFlightWithSplits] =
    CodeShares.uniqueArrivals(paxFeedSourceOrder)

  private def flightToCsvRow(fws: ApiFlightWithSplits, maybeManifest: Option[VoyageManifest]): String = rowValues(fws, maybeManifest).mkString(",")

  def csvStream(flightsStream: Source[(Iterable[ApiFlightWithSplits], VoyageManifests), NotUsed]): Source[String, NotUsed] =
    filterAndSort(flightsStream)
      .map { case (fws, maybeManifest) => flightToCsvRow(fws, maybeManifest) + "\n" }
      .prepend(Source(List(headings + "\n")))

  private def filterAndSort(flightsStream: Source[(Iterable[ApiFlightWithSplits], VoyageManifests), NotUsed],
                           ): Source[(ApiFlightWithSplits, Option[VoyageManifest]), NotUsed] =
    flightsStream.mapConcat { case (flights, manifests) =>
      uniqueArrivalsWithCodeShares(flights.toSeq)
        .filter(fws => flightsFilter(fws, terminals))
        .toSeq
        .sortBy(_.apiFlight.PcpTime.getOrElse(0L))
        .map { fws =>
          val maybeManifest = manifests.manifests.find(_.maybeKey.exists(_ == ManifestKey(fws.apiFlight)))
          (fws, maybeManifest)
        }
    }
}
