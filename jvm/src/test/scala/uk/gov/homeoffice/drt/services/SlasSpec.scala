package uk.gov.homeoffice.drt.services

import org.apache.pekko.actor.{Actor, ActorSystem, Props}
import org.apache.pekko.testkit.TestKit
import org.apache.pekko.util.Timeout
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.homeoffice.drt.actor.commands.Commands.GetState
import uk.gov.homeoffice.drt.ports.Queues.EeaDesk
import uk.gov.homeoffice.drt.ports.config.slas.SlaConfigs
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

import scala.collection.immutable.SortedMap
import scala.concurrent.duration.DurationInt


class MockSlaConfigsActor(slaConfigs: SlaConfigs) extends Actor {
  override def receive: Receive = {
    case GetState => sender() ! slaConfigs
  }
}

class SlasSpec extends TestKit(ActorSystem("SlasSpec")) with AnyWordSpecLike {
  implicit val ec = system.dispatcher
  implicit val timeout = new Timeout(1.second)

  "Sla provider" should {
    "provide SLA for a given date and queue" in {
      val date20231010 = LocalDate(2023, 10, 10)
      val date20240101 = LocalDate(2024, 1, 1)
      val slaConfigs = SlaConfigs(SortedMap(
        SDate(date20231010).millisSinceEpoch -> Map(EeaDesk -> 1),
        SDate(date20240101).millisSinceEpoch -> Map(EeaDesk -> 5),
      ))
      val slaProvider = Slas.slaProvider(system.actorOf(Props(new MockSlaConfigsActor(slaConfigs))))

      assert(slaProvider(date20231010, EeaDesk).futureValue == 1)
      assert(slaProvider(date20240101, EeaDesk).futureValue == 5)
    }
  }
}
