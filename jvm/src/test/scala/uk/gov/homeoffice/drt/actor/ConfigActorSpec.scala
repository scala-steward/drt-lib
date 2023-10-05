package uk.gov.homeoffice.drt.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.homeoffice.drt.actor.commands.Commands.GetState
import uk.gov.homeoffice.drt.actor.commands.CrunchRequest
import uk.gov.homeoffice.drt.ports.Queues.EGate
import uk.gov.homeoffice.drt.ports.config.slas.{SlaConfigs, SlasUpdate}
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import uk.gov.homeoffice.drt.time.SDate

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
  val crunchRequest: MillisSinceEpoch => CrunchRequest = ts => CrunchRequest(SDate(ts).toLocalDate, 0, 1440)

  "A new config actor" must {
    "have empty state" in {
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, crunchRequest, 1)))
      actor ! GetState
      expectMsg(SlaConfigs.empty)
    }
    "contain an update after a SetUpdate command" in {
      val actor = system.actorOf(Props(new ConfigActor("test-id", myNow, crunchRequest, 1)))
      val update = SlasUpdate(1L, Map(EGate -> 1), Option(2L))
      actor ! ConfigActor.SetUpdate(update)
      actor ! GetState
      expectMsg(SlaConfigs(SortedMap(1L -> Map(EGate -> 1))))

      actor ! ConfigActor.RemoveConfig(1L)
    }
  }
}
