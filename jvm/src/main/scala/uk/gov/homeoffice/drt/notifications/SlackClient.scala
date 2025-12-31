package uk.gov.homeoffice.drt.notifications

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.stream.Materializer
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.HttpClient

import scala.concurrent.{ExecutionContext, Future}

trait SlackClient {
  def notify(message: String)(implicit ec: ExecutionContext, mat: Materializer): Future[Unit]
}

object NoopSlackClient extends SlackClient {
  def notify(message: String)(implicit ec: ExecutionContext, mat: Materializer): Future[Unit] = Future.successful()
}

case class SlackClientImpl(httpClient: HttpClient, webhookUrl: String) extends SlackClient {
  private val log = LoggerFactory.getLogger(getClass)

  def notify(message: String)(implicit ec: ExecutionContext, mat: Materializer): Future[Unit] = {
    val payload = s"""{"text": "$message"}"""
    val entity = HttpEntity(ContentTypes.`application/json`, payload)

    val eventualResponse = httpClient.send(HttpRequest(method = HttpMethods.POST, uri = webhookUrl, entity = entity))

    eventualResponse.onComplete({
      case scala.util.Success(HttpResponse(_, _, entity, _)) =>
        entity.dataBytes.runReduce(_ ++ _).foreach { body =>
          log.info(s"Slack response: ${body.utf8String}")
        }
      case scala.util.Failure(t) =>
        log.error(s"Error while sending slack message: $message", t)
    })

    eventualResponse.map(_ => {})
  }
}
