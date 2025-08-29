package uk.gov.homeoffice.drt.ports

import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragment
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2, Terminal}
import uk.gov.homeoffice.drt.ports.config._

class AirportConfigSpec extends Specification {
  "Airport Config" >> {
    Fragment.foreach(for {
      config <- AirportConfigs.allPortConfigs
      (_, terminals) <- config.queuesByTerminal
      terminal <- terminals
      terminalPaxSplits <- config.terminalPaxSplits
    } yield (config, terminal, terminalPaxSplits._2)) {
      case (config, terminal, splitRatios) =>
        s"${config.portCode.iata}, $terminal split ratios should sum to 1" >> {
          splitRatios.splits.map(_.ratio).sum should beCloseTo(1, delta = 0.000001)
        }
    }

    splitOrder(Lhr, T2, List(Queues.EeaDesk, Queues.EGate, Queues.NonEeaDesk))
    splitOrder(Ema, T1, List(Queues.EeaDesk, Queues.EGate, Queues.NonEeaDesk))
    splitOrder(Bhx, T1, List(Queues.EeaDesk, Queues.EGate, Queues.NonEeaDesk))
    splitOrder(Bhx, T2, List(Queues.EeaDesk, Queues.NonEeaDesk))

    Fragment.foreach(for {
      config <- AirportConfigs.allPortConfigs
      terminalAndPaxTypeToQueues <- config.terminalPaxTypeQueueAllocation
    } yield (config, terminalAndPaxTypeToQueues._1, terminalAndPaxTypeToQueues._2.keys.toSet)) {
      case (config, terminal, terminalPaxTypes) =>
        s"${config.portCode.iata}, $terminal should include all pax types in its queue allocations" >> {
          terminalPaxTypes shouldEqual PaxTypes.allPaxTypes.toSet
        }
    }

    Fragment.foreach(AirportConfigs.allPortConfigs) {
      config =>
        s"${config.portCode} should have a valid config" >> {
          config.assertValid()
          true
        }
    }
  }

  private def splitOrder(port: AirportConfigLike, terminalName: Terminal, expectedSplitOrder: List[Queue]): Fragment =
    s"${port.config.portCode} $terminalName split order should be $expectedSplitOrder" >> {
      port.config.queueTypeSplitOrder(terminalName) === expectedSplitOrder
    }
}
