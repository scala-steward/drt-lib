package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.prediction.arrival.features.{Feature, OneToManyFeature, SingleFeature}


case class FeaturesWithOneToManyValues(features: List[Feature[_]], oneToManyValues: IndexedSeq[String]) {
  def oneToManyFeatures: List[OneToManyFeature[_]] = features.collect {
    case otm: OneToManyFeature[_] => otm
  }

  def singleFeatures: Seq[SingleFeature[_]] = features.collect {
    case otm: SingleFeature[_] => otm
  }
}
