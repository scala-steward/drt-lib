package uk.gov.homeoffice.drt

case class ShiftStaffRolling(port: String,
                             terminal: String,
                             rollingStartDate: Long,
                             rollingEndDate: Long,
                             updatedAt: Long,
                             triggeredBy: String)
