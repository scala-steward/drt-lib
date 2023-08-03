package uk.gov.homeoffice.drt.protobuf.serialisation

import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.Nationality
import uk.gov.homeoffice.drt.actor.state.ArrivalsState
import uk.gov.homeoffice.drt.arrivals._
import uk.gov.homeoffice.drt.feeds.{FeedStatus, FeedStatusFailure, FeedStatusSuccess, FeedStatuses}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitSource, SplitSources}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.protobuf.messages.CrunchState._
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage._
import uk.gov.homeoffice.drt.protobuf.messages.Prediction.{PredictionIntMessage, PredictionLongMessage, PredictionsMessage}
import uk.gov.homeoffice.drt.time.SDate

object FlightMessageConversion {
  val log: Logger = LoggerFactory.getLogger(getClass.toString)

  def flightWithSplitsDiffFromMessage(diffMessage: FlightsWithSplitsDiffMessage): FlightsWithSplitsDiff =
    FlightsWithSplitsDiff(diffMessage.updates.map(flightWithSplitsFromMessage).toList, uniqueArrivalsFromMessages(diffMessage.removals))

  def uniqueArrivalToMessage(unique: UniqueArrival): UniqueArrivalMessage =
    UniqueArrivalMessage(Option(unique.number), Option(unique.terminal.toString), Option(unique.scheduled), Option(unique.origin.toString))

  def uniqueArrivalFromMessage(unique: UniqueArrivalMessage): UniqueArrival =
    UniqueArrival(unique.number.getOrElse(0), Terminal(unique.terminalName.getOrElse("")), unique.scheduled.getOrElse(0L), PortCode(unique.origin.getOrElse("")))

  def flightWithSplitsDiffToMessage(diff: FlightsWithSplitsDiff): FlightsWithSplitsDiffMessage = {
    FlightsWithSplitsDiffMessage(
      createdAt = Option(SDate.now().millisSinceEpoch),
      removals = diff.arrivalsToRemove.map {
        case UniqueArrival(number, terminal, scheduled, origin) =>
          UniqueArrivalMessage(Option(number), Option(terminal.toString), Option(scheduled), Option(origin.toString))
        case LegacyUniqueArrival(number, terminal, scheduled) =>
          UniqueArrivalMessage(Option(number), Option(terminal.toString), Option(scheduled), None)
      }.toSeq,
      updates = diff.flightsToUpdate.map(flightWithSplitsToMessage).toSeq
    )
  }

  def arrivalsDiffToMessage(arrivalsDiff: ArrivalsDiff): FlightsDiffMessage = {
    val updateMessages = arrivalsDiff.toUpdate.values.map(apiFlightToFlightMessage).toSeq
    val removalMessages = arrivalsDiff.toRemove.map(a => uniqueArrivalToMessage(a)).toSeq
    FlightsDiffMessage(
      createdAt = Option(SDate.now().millisSinceEpoch),
      removals = removalMessages,
      updates = updateMessages
    )
  }

  def arrivalsDiffFromMessage(flightsDiffMessage: FlightsDiffMessage) = {
    val updates = flightsDiffMessage.updates.map(flightMessageToApiFlight)
    val removals = flightsDiffMessage.removals.map(uniqueArrivalFromMessage)
  }

  def uniqueArrivalsFromMessages(uniqueArrivalMessages: Seq[UniqueArrivalMessage]): Seq[UniqueArrivalLike] =
    uniqueArrivalMessages.collect {
      case UniqueArrivalMessage(Some(number), Some(terminalName), Some(scheduled), Some(origin)) =>
        UniqueArrival(number, terminalName, scheduled, origin)
      case UniqueArrivalMessage(Some(number), Some(terminalName), Some(scheduled), None) =>
        LegacyUniqueArrival(number, terminalName, scheduled)
    }

  def arrivalsStateToSnapshotMessage(state: ArrivalsState): FlightStateSnapshotMessage = {
    val maybeStatusMessages: Option[FeedStatusesMessage] = state.maybeSourceStatuses.flatMap(feedStatuses => feedStatusesToMessage(feedStatuses.feedStatuses))

    FlightStateSnapshotMessage(
      state.arrivals.values.map(apiFlightToFlightMessage).toSeq,
      maybeStatusMessages
    )
  }

  def feedStatusesToMessage(statuses: FeedStatuses): Option[FeedStatusesMessage] = {
    val statusMessages = statuses.statuses.map(feedStatusToMessage)

    Option(FeedStatusesMessage(statusMessages, statuses.lastSuccessAt, statuses.lastFailureAt, statuses.lastUpdatesAt))
  }

  def feedStatusToMessage(feedStatus: FeedStatus): FeedStatusMessage = feedStatus match {
    case s: FeedStatusSuccess => FeedStatusMessage(Option(s.date), Option(s.updateCount), None)
    case s: FeedStatusFailure => FeedStatusMessage(Option(s.date), None, Option(s.message))
  }

  def restoreArrivalsFromSnapshot(restorer: ArrivalsRestorer[Arrival],
                                  snMessage: FlightStateSnapshotMessage): Unit = {
    restorer.applyUpdates(snMessage.flightMessages.map(flightMessageToApiFlight))
  }

  def feedStatusesFromSnapshotMessage(snMessage: FlightStateSnapshotMessage): Option[FeedStatuses] = {
    snMessage.statuses.map(feedStatusesFromFeedStatusesMessage)
  }

  def feedStatusesFromFeedStatusesMessage(message: FeedStatusesMessage): FeedStatuses = FeedStatuses(
    statuses = message.statuses.map(feedStatusFromFeedStatusMessage).toList,
    lastSuccessAt = message.lastSuccessAt,
    lastFailureAt = message.lastFailureAt,
    lastUpdatesAt = message.lastUpdatesAt
  )

  def feedStatusFromFeedStatusMessage(message: FeedStatusMessage): FeedStatus = {
    if (message.updates.isDefined)
      FeedStatusSuccess(message.date.getOrElse(0L), message.updates.getOrElse(0))
    else
      FeedStatusFailure(message.date.getOrElse(0L), message.message.getOrElse("n/a"))
  }

  def flightWithSplitsToMessage(f: ApiFlightWithSplits): FlightWithSplitsMessage = {
    FlightWithSplitsMessage(
      Option(FlightMessageConversion.apiFlightToFlightMessage(f.apiFlight)),
      f.splits.map(apiSplitsToMessage).toList,
      lastUpdated = f.lastUpdated)
  }

  def flightWithSplitsFromMessage(fm: FlightWithSplitsMessage): ApiFlightWithSplits = ApiFlightWithSplits(
    FlightMessageConversion.flightMessageToApiFlight(fm.flight.get),
    fm.splits.map(sm => splitMessageToApiSplits(sm)).toSet,
    lastUpdated = fm.lastUpdated
  )

  def splitMessageToApiSplits(sm: SplitMessage): Splits = {
    val splitSource = SplitSource(sm.source.getOrElse("")) match {
      case SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages_Old => SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages
      case s => s
    }

    Splits(
      sm.paxTypeAndQueueCount.map(ptqcm => {
        ApiPaxTypeAndQueueCount(
          PaxType(ptqcm.paxType.getOrElse("")),
          Queue(ptqcm.queueType.getOrElse("")),
          ptqcm.paxValue.getOrElse(0d),
          nationalitiesFromMessage(ptqcm),
          passengerAgesFromMessage(ptqcm)
        )
      }).toSet,
      splitSource,
      sm.eventType.map(EventType(_)),
      SplitStyle(sm.style.getOrElse("")),
    )
  }


  def nationalitiesFromMessage(ptqcm: PaxTypeAndQueueCountMessage): Option[Map[Nationality, Double]] = ptqcm
    .nationalities
    .map(nc => {
      nc.paxNationality -> nc.count
    }).collect {
    case (Some(nat), Some(count)) => Nationality(nat) -> count
  }.toMap match {
    case nats if nats.isEmpty => None
    case nats => Option(nats)
  }

  def passengerAgesFromMessage(ptqcm: PaxTypeAndQueueCountMessage): Option[Map[PaxAge, Double]] = ptqcm
    .ages
    .map(pa => {
      pa.paxAge -> pa.count
    }).collect {
    case (Some(age), Some(count)) => PaxAge(age) -> count
  }.toMap match {
    case ages if ages.isEmpty => None
    case ages => Option(ages)
  }

  def apiSplitsToMessage(s: Splits): SplitMessage = {
    SplitMessage(
      paxTypeAndQueueCount = s.splits.map(paxTypeAndQueueCountToMessage).toList,
      source = Option(s.source.toString),
      eventType = s.maybeEventType.map(_.toString),
      style = Option(s.splitStyle.name)
    )
  }

  def paxTypeAndQueueCountToMessage(ptqc: ApiPaxTypeAndQueueCount): PaxTypeAndQueueCountMessage = {
    PaxTypeAndQueueCountMessage(
      paxType = Option(ptqc.passengerType.name),
      queueType = Option(ptqc.queueType.toString),
      paxValue = Option(ptqc.paxCount),
      nationalities = Seq(),
      ages = Seq()
    )
  }

  def apiFlightToFlightMessage(apiFlight: Arrival): FlightMessage = {
    val maybePredictionsMessage = Option(PredictionsMessage(
      Option(apiFlight.Predictions.lastChecked),
      apiFlight.Predictions.predictions.map(p => PredictionIntMessage(Option(p._1), Option(p._2))).toList
    ))

    FlightMessage(
      operator = apiFlight.Operator.map(_.code),
      gate = apiFlight.Gate,
      stand = apiFlight.Stand,
      status = Option(apiFlight.Status.description),
      maxPax = apiFlight.MaxPax,
      runwayID = apiFlight.RunwayID,
      baggageReclaimId = apiFlight.BaggageReclaimId,
      airportID = Option(apiFlight.AirportID.iata),
      terminal = Option(apiFlight.Terminal.toString),
      iCAO = Option(apiFlight.flightCodeString),
      iATA = Option(apiFlight.flightCodeString),
      origin = Option(apiFlight.Origin.toString),
      pcpTime = apiFlight.PcpTime,
      feedSources = apiFlight.FeedSources.map(_.toString).toSeq,
      scheduled = Option(apiFlight.Scheduled),
      estimated = apiFlight.Estimated,
      predictions = maybePredictionsMessage,
      touchdown = apiFlight.Actual,
      estimatedChox = apiFlight.EstimatedChox,
      actualChox = apiFlight.ActualChox,
      carrierScheduled = apiFlight.CarrierScheduled,
      redListPax = apiFlight.RedListPax,
      scheduledDeparture = apiFlight.ScheduledDeparture,
      totalPax = convertPassengerSourcesToMessage(apiFlight.PassengerSources)
    )
  }

  def convertPassengerSourcesToMessage(totalPax: Map[FeedSource, Passengers]): Seq[TotalPaxSourceMessage] =
    totalPax.map { case (source, passengers: Passengers) =>
      TotalPaxSourceMessage(feedSource = Option(source.toString),
        passengers = Option(PassengersMessage(passengers.actual, passengers.transit)))
    }.toSeq

  def predictionsToMessage(predictions: Predictions): PredictionsMessage =
    PredictionsMessage(
      updatedAt = Option(predictions.lastChecked),
      predictions = predictions.predictions.map(predictionIntToMessage).toList
    )

  def predictionIntToMessage(maybePred: (String, Int)): PredictionIntMessage =
    PredictionIntMessage(Option(maybePred._1), Option(maybePred._2))

  def predictionLongToMessage(maybePred: Option[Prediction[Long]]): Option[PredictionLongMessage] =
    maybePred.map(pred => PredictionLongMessage(Option(pred.updatedAt), Option(pred.value)))

  def predictionFromMessage(maybePredMessage: Option[PredictionLongMessage]): Option[Prediction[Long]] =
    for {
      predMsg <- maybePredMessage
      updatedAt <- predMsg.updatedAt
      value <- predMsg.value
    } yield Prediction(updatedAt, value)

  def predictionsFromMessage(maybePredictionsMessage: Option[PredictionsMessage]): Predictions =
    maybePredictionsMessage match {
      case None => Predictions(0L, Map())
      case Some(predictions) =>
        val modelPredictions = predictions.predictions.map(msg => (msg.getModelName, msg.getValue))
        Predictions(predictions.updatedAt.getOrElse(0L), modelPredictions.toMap)
    }

  def flightMessageToApiFlight(flightMessage: FlightMessage): Arrival = Arrival(
    Operator = flightMessage.operator.map(Operator),
    Status = ArrivalStatus(flightMessage.status.getOrElse("")),
    Estimated = flightMessage.estimated,
    Predictions = predictionsFromMessage(flightMessage.predictions),
    Actual = flightMessage.touchdown,
    EstimatedChox = flightMessage.estimatedChox,
    ActualChox = flightMessage.actualChox,
    Gate = flightMessage.gate,
    Stand = flightMessage.stand,
    MaxPax = flightMessage.maxPax,
    RunwayID = flightMessage.runwayID,
    BaggageReclaimId = flightMessage.baggageReclaimId,
    AirportID = PortCode(flightMessage.airportID.getOrElse("")),
    Terminal = Terminal(flightMessage.terminal.getOrElse("")),
    rawICAO = flightMessage.iCAO.getOrElse(""),
    rawIATA = flightMessage.iATA.getOrElse(""),
    Origin = PortCode(flightMessage.origin.getOrElse("")),
    PcpTime = flightMessage.pcpTime,
    Scheduled = flightMessage.scheduled.getOrElse(0L),
    FeedSources = getFeedSources(flightMessage.feedSources),
    CarrierScheduled = flightMessage.carrierScheduled,
    RedListPax = flightMessage.redListPax,
    ScheduledDeparture = flightMessage.scheduledDeparture,
    PassengerSources = getPassengerSources(flightMessage)
  )

  private def passengerSourcesFromV1(flightMessage: FlightMessage): Map[FeedSource, Passengers] = {
    val source: FeedSource = if (flightMessage.actualChox.isDefined) LiveFeedSource else UnknownFeedSource
    val paxSources = Map(source -> Passengers(flightMessage.actPaxOLD, flightMessage.tranPaxOLD))

    addApiPaxIfAvailable(flightMessage, paxSources)
  }

  private def addApiPaxIfAvailable(flightMessage: FlightMessage, paxSources: Map[FeedSource, Passengers]): Map[FeedSource, Passengers] = {
    flightMessage.apiPaxOLD match {
      case None => paxSources
      case Some(pax) => paxSources.updated(ApiFeedSource, Passengers(Option(pax), flightMessage.tranPaxOLD))
    }
  }

  private def passengerSourcesFromV2(flightMessage: FlightMessage): Map[FeedSource, Passengers] = {
    val bestSource = bestFeedSource(getFeedSources(flightMessage.feedSources).toSeq)
    val paxSources = flightMessage.feedSources.map {
      case best if best == bestSource.toString => (bestSource, Passengers(flightMessage.actPaxOLD, flightMessage.tranPaxOLD))
      case other => (FeedSource(other).getOrElse(UnknownFeedSource), Passengers(None, None))
    }.toMap

    addApiPaxIfAvailable(flightMessage, paxSources)
  }

  private def passengerSourcesFromV3(flightMessage: FlightMessage): Map[FeedSource, Passengers] = {
    val bestSource = bestFeedSource(getFeedSources(flightMessage.totalPax.map(tp => tp.feedSource.getOrElse(""))).toSeq)
    val paxSources = flightMessage.totalPax.map {
      case TotalPaxSourceMessage(source, _, paxOLD) if source.getOrElse("") == bestSource.toString || source.getOrElse("") == bestSource.name =>
        val feedSource = source.flatMap(FeedSource(_)).getOrElse(UnknownFeedSource)
        (feedSource, Passengers(paxOLD, flightMessage.tranPaxOLD))
      case TotalPaxSourceMessage(source, _, paxOLD) =>
        val feedSource = source.flatMap(FeedSource(_)).getOrElse(UnknownFeedSource)
        (feedSource, Passengers(paxOLD, None))
    }.toMap

    if (!paxSources.contains(ApiFeedSource)) addApiPaxIfAvailable(flightMessage, paxSources) else paxSources
  }

  private def isV1Message(flightMessage: FlightMessage): Boolean =
    flightMessage.totalPax.isEmpty && flightMessage.feedSources.isEmpty

  private def isV2Message(flightMessage: FlightMessage): Boolean =
    flightMessage.totalPax.isEmpty && flightMessage.feedSources.nonEmpty

  private def isV3Message(flightMessage: FlightMessage): Boolean =
    flightMessage.totalPax.exists(_.paxOLD.isDefined)

  def getPassengerSources(flightMessage: FlightMessage): Map[FeedSource, Passengers] = {
    if (isV1Message(flightMessage)) passengerSourcesFromV1(flightMessage)
    else if (isV2Message(flightMessage)) passengerSourcesFromV2(flightMessage)
    else if (isV3Message(flightMessage)) passengerSourcesFromV3(flightMessage)
    else flightMessage.totalPax.map(totalPaxSourceFromMessage).toMap
  }

  private def bestFeedSource(sources: Seq[FeedSource]): FeedSource =
    List(LiveFeedSource, ForecastFeedSource, HistoricApiFeedSource, AclFeedSource)
      .find(sources.contains).getOrElse(UnknownFeedSource)

  private def getFeedSources(sources: Seq[String]): Set[FeedSource] = sources.flatMap(FeedSource(_)).toSet

  def totalPaxSourceFromMessage(message: TotalPaxSourceMessage): (FeedSource, Passengers) = {
    val feedSource = message.feedSource.flatMap(FeedSource(_)).getOrElse(UnknownFeedSource)
    (feedSource, Passengers(message.passengers.flatMap(_.actual), message.passengers.flatMap(_.transit)))
  }

  def flightsToMessage(flights: Iterable[ApiFlightWithSplits]): FlightsWithSplitsMessage =
    FlightsWithSplitsMessage(flights.map(FlightMessageConversion.flightWithSplitsToMessage).toSeq)
}
