package uk.gov.homeoffice.drt

case class ShiftMeta(
                      port: String,
                      terminal: String,
                      shiftAssignmentsMigratedAt: Option[Long]
                    )
