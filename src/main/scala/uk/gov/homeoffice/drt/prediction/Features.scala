package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.prediction.Feature.{Feature, OneToMany, Single}

object Feature {
  sealed trait Feature

  case class Single(columnName: String) extends Feature

  case class OneToMany(columnNames: List[String], featurePrefix: String) extends Feature
}

case class Features(features: List[Feature], oneToManyValues: IndexedSeq[String]) {
  def oneToManyFeatures: Seq[OneToMany] = features.collect {
    case otm: OneToMany => otm
  }

  def singleFeatures: Seq[Single] = features.collect {
    case otm: Single => otm
  }
}
