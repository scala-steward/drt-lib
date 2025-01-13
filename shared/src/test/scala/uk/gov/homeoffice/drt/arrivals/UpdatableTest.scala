package uk.gov.homeoffice.drt.arrivals

import org.scalatest.wordspec.AnyWordSpec

class UpdatableTest extends AnyWordSpec {
  "Arrival" should {
    "not update certain empty fields (bag rec, stand, gate, max pax)" in {
      val arrival = ArrivalGeneratorShared.arrival("BA0001", baggageReclaimId = Option("1"), stand = Option("1"), gate = Option("1"), maxPax = Option(100), status = ArrivalStatus("Scheduled"))
      val updatedArrival = arrival.copy(BaggageReclaimId = None, Stand = None, Gate = None, MaxPax = None, Status = ArrivalStatus("Landed"))
      val updated = arrival.update(updatedArrival)
      assert(updated == arrival.copy(Status = ArrivalStatus("Landed")))
    }
  }
  "ApiFlightWithSplits" should {
    "not update certain empty fields (bag rec, stand, gate, max pax)" in {
      val arrival = ArrivalGeneratorShared.arrival("BA0001", baggageReclaimId = Option("1"), stand = Option("1"), gate = Option("1"), maxPax = Option(100), status = ArrivalStatus("Scheduled"))
      val updatedArrival = arrival.copy(BaggageReclaimId = None, Stand = None, Gate = None, MaxPax = None, Status = ArrivalStatus("Landed"))
      val fws = ApiFlightWithSplits(arrival, Set())
      val updatedFws = fws.copy(apiFlight = updatedArrival)
      val updated = fws.update(updatedFws)
      assert(updated == fws.copy(apiFlight = arrival.copy(Status = ArrivalStatus("Landed"))))
    }
  }
}
