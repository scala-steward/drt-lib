package uk.gov.homeoffice.drt.arrivals

object ArrivalsHeadings {

  final val portCsvHeadings = "IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Predicted Arrival,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages"

  final val rccHeadings = "Region,Port,Terminal," + portCsvHeadings

}
