package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{Feature, OneToMany, Single}


case class FeaturesWithOneToManyValues(features: List[Feature[_]], oneToManyValues: IndexedSeq[String]) {
  def oneToManyFeatures: List[OneToMany[_]] = features.collect {
    case otm: OneToMany[_] => otm
  }

  def singleFeatures: Seq[Single[_]] = features.collect {
    case otm: Single[_] => otm
  }
}
