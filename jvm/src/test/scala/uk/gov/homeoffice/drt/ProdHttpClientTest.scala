package uk.gov.homeoffice.drt

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpRequest, StatusCodes}
import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.auth.Roles

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ProdHttpClientTest extends AnyWordSpec with Matchers {
  "ProdHttpClient" should {
    "create a role header from a list of roles" in {
      val roles = List(Roles.ApiView, Roles.ArrivalsAndSplitsView, Roles.LHR)
      val headers = HttpClient.rolesToRoleHeader(roles)
      headers.length shouldBe 1
      headers.head.name() shouldBe "X-Forwarded-Groups"
      headers.head.value() shouldBe "api:view,arrivals-and-splits:view,lhr"
    }

    "create an empty X-Forwarded-Groups header when given an empty list of roles" in {
      val roles = List()
      val headers = HttpClient.rolesToRoleHeader(roles)
      headers.length shouldBe 1
      headers.head.name() shouldBe "X-Forwarded-Groups"
      headers.head.value() shouldBe ""
    }

    "send an http request" in {
      implicit val system: ActorSystem = ActorSystem()
      implicit val mat: Materializer = Materializer(system)

      val client = ProdHttpClient(request => Http().singleRequest(request))
      val request = HttpRequest(uri = "https://httpbin.org/get")
      val responseFuture = client.send(request)
      val response = Await.result(responseFuture, 10.seconds)

      response.status shouldBe StatusCodes.OK

      system.terminate()
    }
  }
}
