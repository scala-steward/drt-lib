package uk.gov.homeoffice.drt.notifications

import org.apache.pekko.actor.ActorSystem
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SlackClientImplTest extends AnyWordSpec with Matchers {
  "SlackClientImpl" should {
    "be instantiable" in {
      val slackClient = SlackClientImpl(null, "http://example.com/webhook")
      slackClient.webhookUrl shouldBe "http://example.com/webhook"
    }
    "send a message via a mock httpclient" in {
      implicit val system: ActorSystem = org.apache.pekko.actor.ActorSystem()
      implicit val mat: org.apache.pekko.stream.Materializer = org.apache.pekko.stream.Materializer.matFromSystem(org.apache.pekko.actor.ActorSystem())
      val probe = org.apache.pekko.testkit.TestProbe()

      val mockHttpClient = new HttpClient {
        override def send(httpRequest: org.apache.pekko.http.scaladsl.model.HttpRequest): scala.concurrent.Future[org.apache.pekko.http.scaladsl.model.HttpResponse] = {
          httpRequest.entity.dataBytes.runFold("")(_ + _.utf8String).map(body => probe.ref ! body)
          Future.successful(org.apache.pekko.http.scaladsl.model.HttpResponse())
        }
      }
      val slackClient = SlackClientImpl(mockHttpClient, "http://example.com/webhook")
      slackClient.notify("Test message")
      val messageSent = probe.expectMsgType[String]
      messageSent should include("Test message")
    }
  }
}
