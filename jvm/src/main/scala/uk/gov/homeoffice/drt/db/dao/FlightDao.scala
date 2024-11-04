package uk.gov.homeoffice.drt.db.dao

import slick.dbio.Effect
import uk.gov.homeoffice.drt.arrivals.ApiFlightWithSplits
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.db.serialisers.FlightSerialiser
import uk.gov.homeoffice.drt.db.tables.{FlightRow, FlightTable}
import uk.gov.homeoffice.drt.ports.PortCode

import java.sql.Timestamp
import scala.concurrent.ExecutionContext


case class FlightDao(portCode: PortCode)
                    (implicit ec: ExecutionContext) {
  val table: TableQuery[FlightTable] = TableQuery[FlightTable]

  val toRow: ApiFlightWithSplits => FlightRow = FlightSerialiser.toRow(portCode)

  def get(destination: String, origin: String, terminal: String, scheduled: Long, voyageNumber: Int): DBIOAction[Seq[ApiFlightWithSplits], NoStream, Effect.Read] =
    table
      .filter(f =>
        f.origin === origin &&
          f.port === destination &&
          f.terminal === terminal &&
          f.scheduled === new Timestamp(scheduled) &&
          f.voyageNumber === voyageNumber)
      .result
      .map(_.map(FlightSerialiser.fromRow))

  def insertOrUpdate(flight: ApiFlightWithSplits): DBIOAction[Int, NoStream, Effect.Write with Effect.Transactional] =
    table.insertOrUpdate(toRow(flight))
}
