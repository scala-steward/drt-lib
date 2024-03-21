package uk.gov.homeoffice.drt.actor

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import akka.persistence.testkit.{PersistenceTestKitPlugin, PersistenceTestKitSnapshotPlugin}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import uk.gov.homeoffice.drt.actor.TerminalDayFeedArrivalActor.{FeedArrivalsDiff, GetState}
import uk.gov.homeoffice.drt.arrivals.{FeedArrival, FeedArrivalGenerator, UniqueArrival}
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.ports.{AclFeedSource, LiveFeedSource}
import uk.gov.homeoffice.drt.protobuf.messages.FeedArrivalsMessage.{ForecastFeedArrivalsDiffMessage, LiveFeedArrivalsDiffMessage}
import uk.gov.homeoffice.drt.protobuf.serialisation.FeedArrivalMessageConversion

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class TerminalDayFeedArrivalActorTest extends TestKit(ActorSystem("terminal-day-feed-arrival-actor-test-system",
  PersistenceTestKitPlugin.config.withFallback(PersistenceTestKitSnapshotPlugin.config.withFallback(ConfigFactory.load()))))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach {
  private implicit val timeout: Timeout = new Timeout(1.second)

  val testKit: PersistenceTestKit = PersistenceTestKit(system)

  override def beforeEach(): Unit = {
    testKit.clearAll()
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val myNow: () => Long = () => 1L

  "liveDiffToMaybeMessage" should {
    "return a diff message containing the arrival not already existing in the state" in {
      val arrival = FeedArrivalGenerator.live()
      val feedArrivalsDiff = FeedArrivalsDiff(List(arrival), List())
      val maybeMessage = TerminalDayFeedArrivalActor.liveDiffToMaybeMessage(myNow)(feedArrivalsDiff, Map.empty)
      val arrivalMsg = FeedArrivalMessageConversion.liveArrivalToMessage(arrival)

      maybeMessage shouldBe Option(LiveFeedArrivalsDiffMessage(Option(1L), List(), List(arrivalMsg)))
    }
    "return an empty diff message when the arriavl already exists in the state" in {
      val arrival = FeedArrivalGenerator.live()
      val feedArrivalsDiff = FeedArrivalsDiff(List(arrival), List())
      val maybeMessage = TerminalDayFeedArrivalActor.liveDiffToMaybeMessage(myNow)(feedArrivalsDiff, Map(arrival.unique -> arrival))
      maybeMessage.isDefined shouldBe false
    }
  }

  "forecastDiffToMaybeMessage" should {
    "return a diff message containing the arrival not already existing in the state" in {
      val arrival = FeedArrivalGenerator.forecast()
      val feedArrivalsDiff = FeedArrivalsDiff(List(arrival), List())
      val maybeMessage = TerminalDayFeedArrivalActor.forecastDiffToMaybeMessage(myNow)(feedArrivalsDiff, Map.empty)
      val arrivalMsg = FeedArrivalMessageConversion.forecastArrivalToMessage(arrival)

      maybeMessage shouldBe Option(ForecastFeedArrivalsDiffMessage(Option(1L), List(), List(arrivalMsg)))
    }
    "return an empty diff message when the arriavl already exists in the state" in {
      val arrival = FeedArrivalGenerator.forecast()
      val feedArrivalsDiff = FeedArrivalsDiff(List(arrival), List())
      val maybeMessage = TerminalDayFeedArrivalActor.forecastDiffToMaybeMessage(myNow)(feedArrivalsDiff, Map(arrival.unique -> arrival))
      maybeMessage.isDefined shouldBe false
    }
  }

  "forecastStateFromMessage" should {
    "return a map containing the arrival from the message" in {
      val arrival = FeedArrivalGenerator.forecast()
      val arrivalMsg = ForecastFeedArrivalsDiffMessage(Option(1L), List(), List(FeedArrivalMessageConversion.forecastArrivalToMessage(arrival)))
      val state = TerminalDayFeedArrivalActor.forecastStateFromMessage(arrivalMsg, Map.empty)
      state shouldBe Map(arrival.unique -> arrival)
    }
  }

  "liveStateFromMessage" should {
    "return a map containing the arrival from the message" in {
      val arrival = FeedArrivalGenerator.live()
      val arrivalMsg = LiveFeedArrivalsDiffMessage(Option(1L), List(), List(FeedArrivalMessageConversion.liveArrivalToMessage(arrival)))
      val state = TerminalDayFeedArrivalActor.liveStateFromMessage(arrivalMsg, Map.empty)
      state shouldBe Map(arrival.unique -> arrival)
    }
  }

  "TerminalDayFeedArrivalActor for live arrivals" should {
    val arrival = FeedArrivalGenerator.live()
    def props(snapshotThreshold: Int) = Props(TerminalDayFeedArrivalActor.live(2024, 6, 1, T1, LiveFeedSource, None, myNow, snapshotThreshold))
    "respond with an empty map when asked for the latest arrivals" in {
      val terminalDayFeedArrivalActor = system.actorOf(props(2))
      terminalDayFeedArrivalActor ! GetState
      expectMsg(Map.empty[UniqueArrival, FeedArrival])
    }
    "take a FeedArrivalsDiff and respond with the updated state" in {
      val terminalDayFeedArrivalActor = system.actorOf(props(2))
      val feedArrivalsDiff = FeedArrivalsDiff(List(arrival), List())
      Await.ready(terminalDayFeedArrivalActor.ask(feedArrivalsDiff), 1.second)
      terminalDayFeedArrivalActor ! GetState
      expectMsg(Map(arrival.unique -> arrival))
    }
    "recover previously persisted state using a snapshot" in {
      val terminalDayFeedArrivalActor = system.actorOf(props(1))
      val feedArrivalsDiff = FeedArrivalsDiff(List(arrival), List())
      Await.ready(terminalDayFeedArrivalActor.ask(feedArrivalsDiff), 1.second)
      watch(terminalDayFeedArrivalActor)
      terminalDayFeedArrivalActor ! akka.actor.PoisonPill
      expectMsgClass(classOf[akka.actor.Terminated])

      val terminalDayFeedArrivalActor2 = system.actorOf(props(1))

      terminalDayFeedArrivalActor2 ! GetState
      expectMsg(Map(arrival.unique -> arrival))
    }
    "recover previously persisted state using a replaying events" in {
      val terminalDayFeedArrivalActor = system.actorOf(props(2))
      val feedArrivalsDiff = FeedArrivalsDiff(List(arrival), List())
      Await.ready(terminalDayFeedArrivalActor.ask(feedArrivalsDiff), 1.second)
      watch(terminalDayFeedArrivalActor)
      terminalDayFeedArrivalActor ! akka.actor.PoisonPill
      expectMsgClass(classOf[akka.actor.Terminated])

      val terminalDayFeedArrivalActor2 = system.actorOf(props(2))

      terminalDayFeedArrivalActor2 ! GetState
      expectMsg(Map(arrival.unique -> arrival))
    }
  }

  "TerminalDayFeedArrivalActor for forecast arrivals" should {
    val arrival = FeedArrivalGenerator.forecast()
    def props(snapshotThreshold: Int) = Props(TerminalDayFeedArrivalActor.forecast(2024, 6, 1, T1, AclFeedSource, None, myNow, snapshotThreshold))
    "respond with an empty map when asked for the latest arrivals" in {
      val terminalDayFeedArrivalActor = system.actorOf(props(2))
      terminalDayFeedArrivalActor ! GetState
      expectMsg(Map.empty[UniqueArrival, FeedArrival])
    }
    "take a FeedArrivalsDiff and respond with the updated state" in {
      val terminalDayFeedArrivalActor = system.actorOf(props(2))
      val feedArrivalsDiff = FeedArrivalsDiff(List(arrival), List())
      Await.ready(terminalDayFeedArrivalActor.ask(feedArrivalsDiff), 1.second)
      terminalDayFeedArrivalActor ! GetState
      expectMsg(Map(arrival.unique -> arrival))
    }
    "recover previously persisted state using a snapshot" in {
      val terminalDayFeedArrivalActor = system.actorOf(props(1))
      val feedArrivalsDiff = FeedArrivalsDiff(List(arrival), List())
      Await.ready(terminalDayFeedArrivalActor.ask(feedArrivalsDiff), 1.second)
      watch(terminalDayFeedArrivalActor)
      terminalDayFeedArrivalActor ! akka.actor.PoisonPill
      expectMsgClass(classOf[akka.actor.Terminated])

      val terminalDayFeedArrivalActor2 = system.actorOf(props(1))

      terminalDayFeedArrivalActor2 ! GetState
      expectMsg(Map(arrival.unique -> arrival))
    }
    "recover previously persisted state using a replaying events" in {
      val terminalDayFeedArrivalActor = system.actorOf(props(2))
      val feedArrivalsDiff = FeedArrivalsDiff(List(arrival), List())
      Await.ready(terminalDayFeedArrivalActor.ask(feedArrivalsDiff), 1.second)
      watch(terminalDayFeedArrivalActor)
      terminalDayFeedArrivalActor ! akka.actor.PoisonPill
      expectMsgClass(classOf[akka.actor.Terminated])

      val terminalDayFeedArrivalActor2 = system.actorOf(props(2))

      terminalDayFeedArrivalActor2 ! GetState
      expectMsg(Map(arrival.unique -> arrival))
    }
  }
}
