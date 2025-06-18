package uk.gov.homeoffice.drt.protobuf.serialisation

import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.commands._
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.protobuf.messages.CrunchState.{CrunchRequestMessage, MergeArrivalsRequestMessage, RemoveCrunchRequestMessage}
import uk.gov.homeoffice.drt.time.{LocalDate, UtcDate}

object CrunchRequestMessageConversion {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def removeProcessingRequestToMessage(request: RemoveProcessingRequest): GeneratedMessage = {
    val date = request.request.date
    RemoveCrunchRequestMessage(Option(date.year), Option(date.month), Option(date.day), Option(request.request.terminal.toString))
  }

  def terminalUpdateRequestToMessage(cr: TerminalUpdateRequest): CrunchRequestMessage =
    CrunchRequestMessage(
      year = Option(cr.date.year),
      month = Option(cr.date.month),
      day = Option(cr.date.day),
      offsetMinutes = None,
      durationMinutes = None,
      terminalName = Option(cr.terminal.toString),
    )

  def mergeArrivalRequestToMessage(mar: MergeArrivalsRequest): MergeArrivalsRequestMessage = {
    MergeArrivalsRequestMessage(
      year = Option(mar.date.year),
      month = Option(mar.date.month),
      day = Option(mar.date.day),
    )
  }

  def terminalUpdateRequestsFromMessage(terminals: LocalDate => Iterable[Terminal]): CrunchRequestMessage => Seq[TerminalUpdateRequest] = {
    case CrunchRequestMessage(Some(year), Some(month), Some(day), _, _, maybeTerminalName) =>
      maybeTerminalName match {
        case None =>
          terminals(LocalDate(year, month, day)).map(TerminalUpdateRequest(_, LocalDate(year, month, day))).toSeq
        case Some(terminalName) =>
          Seq(TerminalUpdateRequest(Terminal(terminalName), LocalDate(year, month, day)))
      }
  }

  val mergeArrivalsRequestFromMessage: MergeArrivalsRequestMessage => MergeArrivalsRequest = {
    case MergeArrivalsRequestMessage(Some(year), Some(month), Some(day)) =>
      MergeArrivalsRequest(UtcDate(year, month, day))
  }
}
