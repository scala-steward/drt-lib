package uk.gov.homeoffice.drt.notifications

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.apache.pekko.stream.Materializer
import org.apache.pekko.testkit.TestProbe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SlackClientImplTest extends AnyWordSpec with Matchers {
  "SlackClientImpl" should {
    val url = "http://example.com/webhook"

    "be instantiable" in {
      val slackClient = SlackClientImpl(null, url)
      slackClient.webhookUrl shouldBe url
    }
    "send a message via a mock httpclient" in {
      implicit val system: ActorSystem = ActorSystem()
      implicit val mat: Materializer = Materializer.matFromSystem(ActorSystem())
      val probe = TestProbe()

      val mockHttpClient = new HttpClient {
        override def send(httpRequest: org.apache.pekko.http.scaladsl.model.HttpRequest): Future[HttpResponse] = {
          httpRequest.entity.dataBytes.runFold("")(_ + _.utf8String).map(body => probe.ref ! body)
          Future.successful(org.apache.pekko.http.scaladsl.model.HttpResponse())
        }
      }
      val slackClient = SlackClientImpl(mockHttpClient, url)
      slackClient.notify("Test message")
      val messageSent = probe.expectMsgType[String]
      messageSent should include("Test message")
    }
  }
}
