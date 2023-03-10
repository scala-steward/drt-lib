package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.prediction.Feature.{OneToMany, Single}
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.OneToManyFeatureColumn


sealed trait Feature

object Feature {
  case class Single(column: OneToManyFeatureColumn[_]) extends Feature

  case class OneToMany(columns: List[OneToManyFeatureColumn[_]], featurePrefix: String) extends Feature
}

case class FeaturesWithOneToManyValues(features: List[Feature], oneToManyValues: IndexedSeq[String]) {
  def oneToManyFeatures: Seq[OneToMany] = features.collect {
    case otm: OneToMany => otm
  }

  def singleFeatures: Seq[Single] = features.collect {
    case otm: Single => otm
  }
}
