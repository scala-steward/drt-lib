package uk.gov.homeoffice.drt.prediction

import uk.gov.homeoffice.drt.prediction.FeatureType.{FeatureType, OneToMany, Single}

object FeatureType {
  sealed trait FeatureType

  case class Single(columnName: String) extends FeatureType

  case class OneToMany(columnNames: List[String], featurePrefix: String) extends FeatureType
}

case class Features(featureTypes: List[FeatureType], oneToManyValues: IndexedSeq[String]) {
  def oneToManyFeatures: Seq[OneToMany] =
    featureTypes.collect {
      case otm: OneToMany => otm
    }

  def singleFeatures: Seq[Single] =
    featureTypes.collect {
      case otm: Single => otm
    }
}
