package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.prediction.Feature.{OneToMany, Single}


sealed trait Feature

object Feature {
  case class Single(columnName: String) extends Feature

  case class OneToMany(columnNames: List[String], featurePrefix: String) extends Feature
}

case class FeaturesWithOneToManyValues(features: List[Feature], oneToManyValues: IndexedSeq[String]) {
  def oneToManyFeatures: Seq[OneToMany] = features.collect {
    case otm: OneToMany => otm
  }

  def singleFeatures: Seq[Single] = features.collect {
    case otm: Single => otm
  }
}