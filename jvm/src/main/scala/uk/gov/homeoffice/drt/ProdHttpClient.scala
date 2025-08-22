package uk.gov.homeoffice.drt

import org.apache.pekko.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers.Accept
import org.apache.pekko.stream.Materializer
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.HttpClient.rolesToRoleHeader
import uk.gov.homeoffice.drt.auth.Roles
import uk.gov.homeoffice.drt.auth.Roles.Role
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.{ExecutionContext, Future}

trait HttpClient {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def send(httpRequest: HttpRequest): Future[HttpResponse]

  def httpRequestForPortCsv(uri: String, portCode: PortCode): HttpRequest = {
    val roleHeaders = rolesToRoleHeader(List(
      Option(Roles.ArrivalsAndSplitsView), Option(Roles.ApiView), Roles.parse(portCode.iata)
    ).flatten)
    HttpRequest(
      method = HttpMethods.GET,
      uri = uri,
      headers = roleHeaders :+ Accept(MediaTypes.`text/csv`)
    )
  }
}

object HttpClient {
  def rolesToRoleHeader(roles: Iterable[Role]): List[HttpHeader] = {
    val roleHeader: Option[HttpHeader] = HttpHeader
      .parse("X-Forwarded-Groups", roles.map(_.name.toLowerCase).mkString(",")) match {
      case Ok(header, _) => Option(header)
      case _ => None
    }
    roleHeader.toList
  }
}

case class ProdHttpClient(sendHttpRequest: HttpRequest => Future[HttpResponse])(implicit val ec: ExecutionContext, val mat: Materializer) extends HttpClient {
  def send(httpRequest: HttpRequest): Future[HttpResponse] =
    sendHttpRequest(httpRequest)
      .recover {
        case e: Throwable =>
          log.error(s"Failed to connect to ${httpRequest.uri}. ${e.getMessage}")
          throw e
      }
}
