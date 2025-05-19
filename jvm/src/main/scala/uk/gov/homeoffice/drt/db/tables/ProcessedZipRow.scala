package uk.gov.homeoffice.drt.db.tables

import java.sql.Timestamp

case class ProcessedZipRow(zip_file_name: String, success: Boolean, processed_at: Timestamp, created_on: Option[String])
