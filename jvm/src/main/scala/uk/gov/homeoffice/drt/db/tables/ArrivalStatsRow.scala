package uk.gov.homeoffice.drt.db.tables

case class ArrivalStatsRow(portCode: String,
                           terminal: String,
                           date: String,
                           daysAhead: Int,
                           dataType: String,
                           flights: Int,
                           capacity: Int,
                           pax: Int,
                           averageLoad: Double,
                           createdAt: Long,
                          )
