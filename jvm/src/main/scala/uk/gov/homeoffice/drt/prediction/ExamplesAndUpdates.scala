package uk.gov.homeoffice.drt.prediction

import akka.NotUsed
import akka.stream.scaladsl.Source
import uk.gov.homeoffice.drt.actor.PredictionModelActor.ModelUpdate
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.SDateLike

import scala.concurrent.Future

trait ExamplesAndUpdates[MI] {
  val persistenceType: String
  val modelIdWithExamples: (Terminal, SDateLike, Int) => Source[(MI, Iterable[(Double, Seq[String])]), NotUsed]
  val updateModel: (MI, Option[ModelUpdate]) => Future[_]
}


