package uk.gov.homeoffice.drt.actor

import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.PredictionModelActor.{ModelUpdate, TerminalFlightNumberOrigin}
import uk.gov.homeoffice.drt.prediction.arrival.features.FeatureColumnsV1.DayOfWeek
import uk.gov.homeoffice.drt.prediction.arrival.OffScheduleModelAndFeatures
import uk.gov.homeoffice.drt.prediction.category.FlightCategory
import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, RegressionModel}
import uk.gov.homeoffice.drt.protobuf.messages.ModelAndFeatures.ModelAndFeaturesMessage
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class MockPredictionModelActor(probeRef: ActorRef) extends PredictionModelActor(() => SDate.now(), FlightCategory, TerminalFlightNumberOrigin("T1", 100, "JFK"), None) {
  override def persistAndMaybeSnapshotWithAck(messageToPersist: GeneratedMessage,
                                              maybeAck:List[(ActorRef, Any)],
                                              maybePostPersist: Option[() => Future[_]] = None,
                                             ): Unit = {
    probeRef ! messageToPersist
  }
}

class PredictionModelActorTest extends TestKit(ActorSystem("Predictions"))
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A PredictionModel actor" should {
    implicit val sdateProvider: Long => SDateLike = (ts: Long) => SDate(ts)
    val features = FeaturesWithOneToManyValues(List(DayOfWeek()), IndexedSeq("t", "h", "u"))
    val modelUpdate = ModelUpdate(RegressionModel(Seq(1, 2), 1.4), OffScheduleModelAndFeatures.featuresVersion, features, 10, 10.1, OffScheduleModelAndFeatures.targetName)

    "Persist an incoming model" in {
      val probe = TestProbe()
      val actor = system.actorOf(Props(new MockPredictionModelActor(probe.ref)))
      actor ! modelUpdate
      probe.expectMsgClass(classOf[ModelAndFeaturesMessage])
    }

    "Not persist an incoming model if it's the same as the last one received" in {
      val probe = TestProbe()
      val actor = system.actorOf(Props(new MockPredictionModelActor(probe.ref)))
      actor ! modelUpdate
      probe.expectMsgClass(classOf[ModelAndFeaturesMessage])
      actor ! modelUpdate
      probe.expectNoMessage(250.milliseconds)
    }
  }
}
