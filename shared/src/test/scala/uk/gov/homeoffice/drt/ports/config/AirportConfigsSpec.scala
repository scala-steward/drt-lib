package uk.gov.homeoffice.drt.ports.config

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.auth.Roles.LHR
import uk.gov.homeoffice.drt.ports.Terminals.{T1, Terminal}
import uk.gov.homeoffice.drt.ports._

import scala.collection.immutable.SortedMap

class AirportConfigsSpec extends Specification {

  "AirportConfigs" should {

    "have a list size of 24 of min and max desks by terminal and queue for all ports" in {
      for {
        port <- AirportConfigs.allPortConfigs
        terminalName <- port.minMaxDesksByTerminalQueue24Hrs.keySet
        queueName <- port.minMaxDesksByTerminalQueue24Hrs(terminalName).keySet
        (minDesks, maxDesks) = port.minMaxDesksByTerminalQueue24Hrs(terminalName)(queueName)
      } yield {
        minDesks.size.aka(s"minDesk-> ${port.portCode} -> $terminalName -> $queueName") mustEqual 24
        maxDesks.size.aka(s"maxDesk-> ${port.portCode} -> $terminalName -> $queueName") mustEqual 24
      }
    }

    "Queue names in min max desks by terminal and queues should be defined in Queues" in {
      for {
        port <- AirportConfigs.allPortConfigs
        terminalName <- port.minMaxDesksByTerminalQueue24Hrs.keySet
        queueName <- port.minMaxDesksByTerminalQueue24Hrs(terminalName).keySet
      } yield {
        Queues.displayName(queueName).aka(s"$queueName not found in Queues") mustNotEqual None
      }
    }

    "All Airport config queues must be defined in Queues" in {
      for {
        port <- AirportConfigs.allPortConfigs
        queueName <- port.queuesByTerminal.values.flatten
      } yield {
        Queues.displayName(queueName).aka(s"$queueName not found in Queues") mustNotEqual None
      }
    }

    "All configurations should be valid with no missing queues or terminals" in {
      AirportConfigs.allPortConfigs.foreach(_.assertValid())

      success
    }
  }

}
