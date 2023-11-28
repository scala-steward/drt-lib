package uk.gov.homeoffice.drt.training


import upickle.default._

case class FeatureGuide(id: Option[Int], uploadTime: Long, fileName: Option[String], title: Option[String], markdownContent: String, published: Boolean)

object FeatureGuide {

  implicit val rw: ReadWriter[FeatureGuide] = macroRW

  def deserializeFromJsonString(string: String): Seq[FeatureGuide] = read[Seq[FeatureGuide]](string)

  def serializeToJsonString(featureGuides: Seq[FeatureGuide]): String = write(featureGuides)
}
