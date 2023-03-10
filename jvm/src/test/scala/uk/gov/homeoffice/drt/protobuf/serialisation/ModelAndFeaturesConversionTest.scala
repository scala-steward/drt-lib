package uk.gov.homeoffice.drt.protobuf.serialisation

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.prediction.Feature.{OneToMany, Single}
import uk.gov.homeoffice.drt.prediction.arrival.FeatureColumns.{BestPax, DayOfWeek, PartOfDay}
import uk.gov.homeoffice.drt.prediction.arrival.OffScheduleModelAndFeatures
import uk.gov.homeoffice.drt.prediction.{FeaturesWithOneToManyValues, ModelAndFeatures, RegressionModel}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

class ModelAndFeaturesConversionTest extends Specification {
  implicit val sdateProvider: Long => SDateLike = (ts: Long) => SDate(ts)

  "Given a ModelAndFeatures class" >> {
    "I should be able to serialise and deserialise it back to its original form" >> {
      val model = RegressionModel(Seq(1, 2, 3), -1.45)

      val features = FeaturesWithOneToManyValues(List(OneToMany(List(DayOfWeek(), PartOfDay()), "_a"), Single(BestPax)), IndexedSeq("aa", "bb", "cc"))
      val modelAndFeatures = ModelAndFeatures(model, features, OffScheduleModelAndFeatures.targetName, 100, 10.1)

      val serialised = ModelAndFeaturesConversion.modelAndFeaturesToMessage(modelAndFeatures, 0L)

      val deserialised = ModelAndFeaturesConversion.modelAndFeaturesFromMessage(serialised)

      deserialised === modelAndFeatures
    }
  }
  "Given an OffScheduleModelAndFeatures class" >> {
    "I should be able to serialise and deserialise it back to its original form" >> {
      val model = RegressionModel(Seq(1, 2, 3), -1.45)
      val features = FeaturesWithOneToManyValues(List(OneToMany(List(DayOfWeek(), PartOfDay()), "_a"), Single(BestPax)), IndexedSeq("aa", "bb", "cc"))
      val modelAndFeatures = OffScheduleModelAndFeatures(model, features, 100, 10.1.toInt)

      val serialised = ModelAndFeaturesConversion.modelAndFeaturesToMessage(modelAndFeatures, 0L)

      val deserialised = ModelAndFeaturesConversion.modelAndFeaturesFromMessage(serialised)

      deserialised === modelAndFeatures
    }
  }
  "Two ModelAndFeatures with different values should not be equal" >> {
    val features = FeaturesWithOneToManyValues(List(OneToMany(List(DayOfWeek(), PartOfDay()), "_a"), Single(BestPax)), IndexedSeq("aa", "bb", "cc"))
    val model1 = RegressionModel(Seq(1, 2, 3), -1.45)
    val model2 = RegressionModel(Seq(2, 3, 4), 0.20)
    val modelAndFeatures1 = OffScheduleModelAndFeatures(model1, features, 100, 10.1.toInt)
    val modelAndFeatures2 = OffScheduleModelAndFeatures(model2, features, 100, 20.1.toInt)

    val areEqual = (modelAndFeatures1, modelAndFeatures2) match {
      case (m1: ModelAndFeatures, m2: ModelAndFeatures) => m1 == m2
    }

    areEqual === false
  }
}
