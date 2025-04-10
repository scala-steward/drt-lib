package uk.gov.homeoffice.drt.actor

import org.apache.pekko.pattern.StatusReply.Ack
import org.apache.pekko.persistence.{RecoveryCompleted, SnapshotMetadata, SnapshotOffer}
import org.apache.spark.ml.regression.LinearRegressionModel
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.PredictionModelActor.{Models, RemoveModel, WithId}
import uk.gov.homeoffice.drt.actor.commands.Commands.GetState
import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, ModelAndFeatures, ModelCategory, RegressionModel}
import uk.gov.homeoffice.drt.protobuf.messages.ModelAndFeatures.{ModelAndFeaturesMessage, ModelsAndFeaturesMessage, RemoveModelMessage}
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

import scala.util.{Failure, Try}

object PredictionModelActor {
  case class Models(models: Map[String, ModelAndFeatures])

  case class ModelUpdate(model: RegressionModel,
                         featuresVersion: Int,
                         features: FeaturesWithOneToManyValues,
                         examplesTrainedOn: Int,
                         improvementPct: Double,
                         targetName: String,
                        ) extends ModelAndFeatures

  case class RemoveModel(targetName: String)

  object RegressionModelFromSpark {
    def apply(lrModel: LinearRegressionModel): RegressionModel = RegressionModel(lrModel.coefficients.toArray, lrModel.intercept)
  }

  trait WithId {
    val id: String
  }

  case class Terminal(terminal: String) extends WithId {
    val id = s"terminal-$terminal"
  }

  object Terminal {
    val fromArrival: Arrival => Option[Terminal] = (arrival: Arrival) =>
      Option(Terminal(arrival.Terminal.toString))
  }

  case class TerminalCarrier(terminal: String, carrier: String) extends WithId {
    val id = s"terminal-carrier-$terminal-$carrier"
  }

  object TerminalCarrier {
    val fromArrival: Arrival => Option[TerminalCarrier] = (arrival: Arrival) =>
      Option(TerminalCarrier(arrival.Terminal.toString, arrival.CarrierCode.code))
  }

  case class TerminalOrigin(terminal: String, origin: String) extends WithId {
    val id = s"terminal-origin-$terminal-$origin"
  }

  object TerminalOrigin {
    val fromArrival: Arrival => Option[TerminalOrigin] = (arrival: Arrival) =>
      Option(TerminalOrigin(arrival.Terminal.toString, arrival.Origin.iata))
  }

  case class TerminalFlightNumberOrigin(terminal: String, number: Int, origin: String) extends WithId {
    val id = s"terminal-flightnumber-origin-$terminal-$number-$origin"
  }

  object TerminalFlightNumberOrigin {
    val fromArrival: Arrival => Option[WithId] = (arrival: Arrival) => {
      val flightNumber = arrival.flightCode.voyageNumberLike.numeric
      Option(TerminalFlightNumberOrigin(arrival.Terminal.toString, flightNumber, arrival.Origin.iata))
    }
  }

  case class TerminalCarrierOrigin(terminal: String, carrier: String, origin: String) extends WithId {
    val id = s"terminal-carrier-origin-$terminal-$carrier-$origin"
  }

  object TerminalCarrierOrigin {
    val fromArrival: Arrival => Option[TerminalCarrierOrigin] = (arrival: Arrival) => {
      val carrierCode = arrival.flightCode.carrierCode.code
      Option(TerminalCarrierOrigin(arrival.Terminal.toString, carrierCode, arrival.Origin.iata))
    }
  }
}

class PredictionModelActor(val now: () => SDateLike,
                           modelCategory: ModelCategory,
                           identifier: WithId,
                           override val maybePointInTime: Option[Long],
                          ) extends RecoveryActorLike {

  import uk.gov.homeoffice.drt.protobuf.serialisation.ModelAndFeaturesConversion._

  override val log: Logger = LoggerFactory.getLogger(getClass)

  override val recoveryStartMillis: Long = now().millisSinceEpoch
  override val maybeSnapshotInterval: Option[Int] = Option(100)

  var state: Map[String, ModelAndFeatures] = Map()

  override def persistenceId: String = s"${modelCategory.name}-prediction-${identifier.id}".toLowerCase

  implicit val sdateFromLong: Long => SDateLike = (ts: Long) => SDate(ts)
  implicit val sdateFromLocalDate: LocalDate => SDateLike = (ts: LocalDate) => SDate(ts)

  override def logSnapshotOffer(md: SnapshotMetadata): Unit = log.info(snapshotOfferLogMessage(md))

  override def processRecoveryMessage: PartialFunction[Any, Unit] = {
    case RemoveModelMessage(targetName, Some(createdAt)) =>
      maybePointInTime match {
        case Some(pit) if pit < createdAt =>
          log.debug(s"Ignoring message created more recently than the recovery point in time")
        case _ =>
          targetName.foreach(tn => state = state - tn)
      }

    case msg: ModelAndFeaturesMessage =>
      (maybePointInTime, msg.timestamp) match {
        case (Some(pit), Some(createdAt)) if pit < createdAt =>
          log.debug(s"Ignoring message created more recently than the recovery point in time")
        case _ =>
          modelAndFeaturesFromMessage(msg).foreach(modelAndFeatures =>
            state = state.updated(modelAndFeatures.targetName, modelAndFeatures)
          )
      }
  }

  override def processSnapshotMessage: PartialFunction[Any, Unit] = {
    case msg: ModelsAndFeaturesMessage =>
      state = modelsAndFeaturesFromMessage(msg).map { maf =>
        maf.targetName -> maf
      }.toMap
  }

  override def stateToMessage: GeneratedMessage =
    modelsAndFeaturesToMessage(state.values, now().millisSinceEpoch)

  override def receiveCommand: Receive = {
    case GetState =>
      sender() ! Models(state)

    case maf: ModelAndFeatures =>
      val isUpdated = state.get(maf.targetName) match {
        case Some(existingMaf) => maf != existingMaf
        case None => true
      }
      if (isUpdated) {
        state = state.updated(maf.targetName, maf)
        val replyToAndAck = List((sender(), Ack))
        persistAndMaybeSnapshotWithAck(modelAndFeaturesToMessage(maf, now().millisSinceEpoch), replyToAndAck)
      } else {
        sender() ! Ack
      }

    case RemoveModel(targetName) =>
      if (state.contains(targetName)) {
        state = state - targetName
        val replyToAndAck = List((sender(), Ack))
        persistAndMaybeSnapshotWithAck(RemoveModelMessage(Option(targetName), Option(now().millisSinceEpoch)), replyToAndAck)
      } else {
        sender() ! Ack
      }
  }
}


