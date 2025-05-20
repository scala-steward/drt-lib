package uk.gov.homeoffice.drt.db.dao

import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.Nationality
import uk.gov.homeoffice.drt.arrivals.CarrierCode
import uk.gov.homeoffice.drt.arrivals.EventTypes.DC
import uk.gov.homeoffice.drt.db.CentralDatabase
import uk.gov.homeoffice.drt.models._
import uk.gov.homeoffice.drt.ports.{PaxAge, PortCode}
import uk.gov.homeoffice.drt.time.SDate

import scala.concurrent.{ExecutionContext, Future}

object ApiManifestProvider {
  private val log = LoggerFactory.getLogger(getClass)

  def apply(tables: CentralDatabase)(implicit ec: ExecutionContext): UniqueArrivalKey => Future[Option[VoyageManifest]] =
    uniqueArrivalKey => {

      import tables.profile.api._

      val scheduled = SDate(uniqueArrivalKey.scheduled.millisSinceEpoch).toISOString
      val query =
        sql"""SELECT
              document_type,
              document_issuing_country_code,
              eea_flag,
              age,
              disembarkation_port_code,
              in_transit_flag,
              disembarkation_port_country_code,
              nationality_country_code,
              passenger_identifier
            FROM voyage_manifest_passenger_info
            WHERE
              event_code ='DC'
              and arrival_port_code=${uniqueArrivalKey.arrivalPort.iata}
              and departure_port_code=${uniqueArrivalKey.departurePort.iata}
              and voyage_number=${uniqueArrivalKey.voyageNumber.numeric}
              and scheduled_date = TIMESTAMP '#$scheduled'
            """.as[(String, String, String, Int, String, String, String, String, String)]
          .map {
            _.map {
              case (dt, dcc, eea, age, disPc, it, disPcc, natCc, pId) =>
                PassengerInfoJson(
                  DocumentType = Option(DocumentType(dt)),
                  DocumentIssuingCountryCode = Nationality(dcc),
                  EEAFlag = EeaFlag(eea),
                  Age = Option(PaxAge(age)),
                  DisembarkationPortCode = Option(PortCode(disPc)),
                  InTransitFlag = InTransit(it),
                  DisembarkationPortCountryCode = Option(Nationality(disPcc)),
                  NationalityCountryCode = Option(Nationality(natCc)),
                  PassengerIdentifier = if (pId.isEmpty) None else Option(pId)
                )
            }
          }

      tables.db.run(query).map {
          case pax if pax.isEmpty => None
          case pax =>
            Option(VoyageManifest(
              DC,
              uniqueArrivalKey.arrivalPort,
              uniqueArrivalKey.departurePort,
              uniqueArrivalKey.voyageNumber,
              CarrierCode(""),
              ManifestDateOfArrival(uniqueArrivalKey.scheduled.toISODateOnly),
              ManifestTimeOfArrival(uniqueArrivalKey.scheduled.toHoursAndMinutes),
              pax.toList
            ))
        }
        .recover {
          case t =>
            log.error(s"Failed to execute query", t)
            None
        }

    }
}
