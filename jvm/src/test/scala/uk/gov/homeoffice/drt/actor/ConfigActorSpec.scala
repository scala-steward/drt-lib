package uk.gov.homeoffice.drt.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.homeoffice.drt.actor.commands.Commands.GetState
import uk.gov.homeoffice.drt.actor.commands.{Commands, CrunchRequest}
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk}
import uk.gov.homeoffice.drt.ports.config.slas.{SlaConfigs, SlasUpdate}
import uk.gov.homeoffice.drt.time.TimeZoneHelper.europeLondonTimeZone
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

import scala.collection.immutable.SortedMap

class ConfigActorSpec
  extends TestKit(ActorSystem("ConfigActorSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val myNow: () => SDate.JodaSDate = () => SDate.now()
  val crunchRequest: LocalDate => CrunchRequest = date => CrunchRequest(SDate(date).toLocalDate, 0, 1440)

  "A new config actor" must {
    "have empty state" in {
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, crunchRequest, 1)))
      actor ! GetState
      expectMsg(SlaConfigs.empty)
    }

    "contain a config after a SetUpdate command, and be empty after a RemoveConfig command" in {
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, crunchRequest, 1)))
      val update = SlasUpdate(1L, Map(EGate -> 1), Option(2L))
      actor ! ConfigActor.SetUpdate(update)
      actor ! GetState
      expectMsg(SlaConfigs(SortedMap(1L -> Map(EGate -> 1))))

      actor ! ConfigActor.RemoveConfig(1L)
      actor ! GetState
      expectMsg(SlaConfigs.empty)
    }

    "replace a config after a SetUpdate command" in {
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, crunchRequest, 1)))
      val update = SlasUpdate(1L, Map(EGate -> 1), None)
      val update2 = SlasUpdate(2L, Map(EeaDesk -> 10), Option(1L))
      actor ! ConfigActor.SetUpdate(update)
      actor ! GetState
      expectMsg(SlaConfigs(SortedMap(1L -> Map(EGate -> 1))))

      actor ! ConfigActor.SetUpdate(update2)
      actor ! GetState
      expectMsg(SlaConfigs(SortedMap(2L -> Map(EeaDesk -> 10))))
    }

    "contain two configs after 2 SetUpdate commands" in {
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, crunchRequest, 1)))
      val update = SlasUpdate(1L, Map(EGate -> 1), None)
      val update2 = SlasUpdate(2L, Map(EeaDesk -> 10), None)
      actor ! ConfigActor.SetUpdate(update)
      actor ! ConfigActor.SetUpdate(update2)
      actor ! GetState
      expectMsg(SlaConfigs(SortedMap(
        1L -> Map(EGate -> 1),
        2L -> Map(EeaDesk -> 10),
      )))
    }

    "send the updates subscriber a crunch request starting at the first date effected" in {
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, crunchRequest, 1)))
      val updateDate = SDate("2023-10-10T00:00", europeLondonTimeZone)
      val update = SlasUpdate(updateDate.millisSinceEpoch, Map(EGate -> 1), None)
      val testProbe = TestProbe()
      actor ! Commands.AddUpdatesSubscriber(testProbe.ref)
      actor ! ConfigActor.SetUpdate(update)
      testProbe.expectMsg(CrunchRequest(LocalDate(2023, 10, 10), 0, 1440))
      testProbe.expectMsg(CrunchRequest(LocalDate(2023, 10, 11), 0, 1440))
    }
  }
}
