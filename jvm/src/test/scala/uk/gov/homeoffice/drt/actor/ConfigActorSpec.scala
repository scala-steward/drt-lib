package uk.gov.homeoffice.drt.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.homeoffice.drt.actor.commands.Commands.GetState
import uk.gov.homeoffice.drt.actor.commands.{Commands, CrunchRequest, TerminalUpdateRequest}
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk}
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.ports.config.slas.{SlaConfigs, SlasUpdate}
import uk.gov.homeoffice.drt.time.TimeZoneHelper.europeLondonTimeZone
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

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

  val myNow: () => SDateLike = () => SDate("2023-10-10T00:00")
  val updateRequest: LocalDate => Seq[TerminalUpdateRequest] = date => Seq(TerminalUpdateRequest(T1, SDate(date).toLocalDate))

  "A new config actor" must {
    "have empty state" in {
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, updateRequest, 1)))
      actor ! GetState
      expectMsg(SlaConfigs.empty)
    }

    "contain a config after a SetUpdate command, and be empty after a RemoveConfig command" in {
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, updateRequest, 1)))
      val update = SlasUpdate(1L, Map(EGate -> 1), Option(2L))
      actor ! ConfigActor.SetUpdate(update)
      actor ! GetState
      expectMsg(SlaConfigs(SortedMap(1L -> Map(EGate -> 1))))

      actor ! ConfigActor.RemoveConfig(1L)
      actor ! GetState
      expectMsg(SlaConfigs.empty)
    }

    "replace a config after a SetUpdate command" in {
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, updateRequest, 1)))
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
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, updateRequest, 1)))
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

    "send the updates subscriber a crunch request starting at the first date non-historic date effected (11 Oct)" in {
      val yesterday = SDate("2023-10-10T00:00", europeLondonTimeZone)
      val today = SDate("2023-10-11T00:00", europeLondonTimeZone)

      val actor = system.actorOf(Props(new ConfigActor("test-id", () => today, updateRequest, 1)))
      val update = SlasUpdate(yesterday.millisSinceEpoch, Map(EGate -> 1), None)
      val testProbe = TestProbe()

      actor ! Commands.AddUpdatesSubscriber(testProbe.ref)
      actor ! ConfigActor.SetUpdate(update)

      testProbe.expectMsg(TerminalUpdateRequest(T1, LocalDate(2023, 10, 11)))
      testProbe.expectMsg(TerminalUpdateRequest(T1, LocalDate(2023, 10, 12)))
    }
  }
}
