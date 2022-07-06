package uk.gov.homeoffice.drt.ports

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._

class PaxTypesAndQueuesTest extends AnyWordSpec with Matchers {
  "PaxTypesAndQueues" should {
    "have matching entries in in-order" in {
      val intersect = PaxTypesAndQueues.inOrder.toSet.intersect(allPaxTypeAndQueues)
      intersect should ===(PaxTypesAndQueues.allPaxTypeAndQueues - transitToTransfer)
    }
  }
}
