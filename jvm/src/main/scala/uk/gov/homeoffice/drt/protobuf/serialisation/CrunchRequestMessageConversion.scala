package uk.gov.homeoffice.drt.protobuf.serialisation

import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.commands._
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.protobuf.messages.CrunchState.{CrunchRequestMessage, MergeArrivalsRequestMessage, RemoveCrunchRequestMessage, RemoveMergeArrivalsRequestMessage}
import uk.gov.homeoffice.drt.time.{LocalDate, UtcDate}

object CrunchRequestMessageConversion {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def removeProcessingRequestToMessage(request: RemoveProcessingRequest): GeneratedMessage = {
    val date = request.request.date
    request.request match {
      case _: CrunchRequest =>
        RemoveCrunchRequestMessage(Option(date.year), Option(date.month), Option(date.day))
      case _: MergeArrivalsRequest =>
        RemoveMergeArrivalsRequestMessage(Option(date.year), Option(date.month), Option(date.day))
    }
  }

  def loadProcessingRequestToMessage(cr: LoadProcessingRequest): CrunchRequestMessage = {
    val maybeTerminalName = cr match {
      case _: CrunchRequest => None
      case tur: TerminalUpdateRequest => Option(tur.terminal.toString)
    }
    CrunchRequestMessage(
      year = Option(cr.date.year),
      month = Option(cr.date.month),
      day = Option(cr.date.day),
      offsetMinutes = None,
      durationMinutes = None,
      terminalName = maybeTerminalName,
    )
  }

  def mergeArrivalRequestToMessage(mar: MergeArrivalsRequest): MergeArrivalsRequestMessage = {
    MergeArrivalsRequestMessage(
      year = Option(mar.date.year),
      month = Option(mar.date.month),
      day = Option(mar.date.day),
    )
  }

  val loadProcessingRequestFromMessage: CrunchRequestMessage => LoadProcessingRequest = {
    case CrunchRequestMessage(Some(year), Some(month), Some(day), _, _, maybeTerminalName) =>
      maybeTerminalName match {
        case None =>
          CrunchRequest(LocalDate(year, month, day))
        case Some(terminalName) =>
          TerminalUpdateRequest(Terminal(terminalName), LocalDate(year, month, day))
      }
  }

  val mergeArrivalsRequestFromMessage: MergeArrivalsRequestMessage => MergeArrivalsRequest = {
    case MergeArrivalsRequestMessage(Some(year), Some(month), Some(day)) =>
      MergeArrivalsRequest(UtcDate(year, month, day))
  }
}
