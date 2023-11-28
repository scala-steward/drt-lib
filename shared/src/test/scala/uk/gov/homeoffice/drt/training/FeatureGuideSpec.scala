package uk.gov.homeoffice.drt.training

import org.specs2.mutable.Specification

class FeatureGuideSpec extends Specification {

  val featureGuides = List(
    FeatureGuide(Some(1), 1686066599088l, Some("test1"), Some("Test1"), "Here is markdown example", true),
    FeatureGuide(Some(2), 1686066891940l, Some("test1"), Some("Test1"), "Here is markdown example", true),
    FeatureGuide(Some(3), 1686068871259l, Some("test1"), Some("Test1"), "Here is markdown example", true),
    FeatureGuide(Some(4), 1686069026706l, Some("test1"), Some("Test1"), "Here is markdown example", true),
    FeatureGuide(Some(5), 1686069182159l, Some("test1"), Some("Test1"), "Here is markdown example", true),
    FeatureGuide(Some(6), 1686069368240l, Some("test1"), Some("Test1"), "Here is markdown example", true),
    FeatureGuide(Some(7), 1686069689034l, Some("test1"), Some("Test1"), "Here is markdown example", true),
    FeatureGuide(Some(8), 1686070223212l, Some("test2.mov"), Some("Test2"), "Here is markdown example test3", true),
    FeatureGuide(Some(9), 1686070683558l, Some("test2.mov"), Some("RTest4"), "Here is markdown example test4\r\n\r\n- Some information 1\r\n- Some information 2 \r\n- Some information 3", true))

  val featureGuidesString =
    """
      |[{"id":[1],"uploadTime":1686066599088,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published": true},
      |{"id":[2],"uploadTime":1686066891940,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published": true},
      |{"id":[3],"uploadTime":1686068871259,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published": true},
      |{"id":[4],"uploadTime":1686069026706,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published": true},
      |{"id":[5],"uploadTime":1686069182159,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published": true},
      |{"id":[6],"uploadTime":1686069368240,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published": true},
      |{"id":[7],"uploadTime":1686069689034,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published": true},
      |{"id":[8],"uploadTime":1686070223212,"fileName":["test2.mov"],"title":["Test2"],"markdownContent":"Here is markdown example test3","published": true},
      |{"id":[9],"uploadTime":1686070683558,"fileName":["test2.mov"],"title":["RTest4"],"markdownContent":"Here is markdown example test4\r\n\r\n- Some information 1\r\n- Some information 2 \r\n- Some information 3","published": true}]
      |""".stripMargin


  val featureGuideResult =
    """
      |[{"id":[1],"uploadTime":1686066599088,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published":true},
      |{"id":[2],"uploadTime":1686066891940,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published":true},
      |{"id":[3],"uploadTime":1686068871259,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published":true},
      |{"id":[4],"uploadTime":1686069026706,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published":true},
      |{"id":[5],"uploadTime":1686069182159,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published":true},
      |{"id":[6],"uploadTime":1686069368240,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published":true},
      |{"id":[7],"uploadTime":1686069689034,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published":true},
      |{"id":[8],"uploadTime":1686070223212,"fileName":["test2.mov"],"title":["Test2"],"markdownContent":"Here is markdown example test3","published":true},
      |{"id":[9],"uploadTime":1686070683558,"fileName":["test2.mov"],"title":["RTest4"],"markdownContent":"Here is markdown example test4\r\n\r\n- Some information 1\r\n- Some information 2 \r\n- Some information 3","published":true}]
      |""".stripMargin

  "Given sequence of FeatureGuide json string" >> {
    "Then I should be able to serialise and de-serialise it" >> {
      val a = FeatureGuide.deserializeFromJsonString(featureGuidesString)
      a must beAnInstanceOf[Seq[FeatureGuide]]
      a mustEqual featureGuides
    }
  }

  "Give sequence of FeatureGuide Object I get Json seq" >> {
    "Then I should be able to serialise and de-serialise it" >> {
      val featureGuides = List(
        FeatureGuide(Some(1), 1686066599088l, Some("test1"), Some("Test1"), "Here is markdown example", true),
        FeatureGuide(Some(2), 1686066891940l, Some("test2"), Some("Test3"), "Here is markdown example", true))
      val featureGuidesString = """[{"id":[1],"uploadTime":1686066599088,"fileName":["test1"],"title":["Test1"],"markdownContent":"Here is markdown example","published":true},{"id":[2],"uploadTime":1686066891940,"fileName":["test2"],"title":["Test3"],"markdownContent":"Here is markdown example","published":true}]"""
      val a = FeatureGuide.serializeToJsonString(featureGuides)
      a must beAnInstanceOf[String]
      a mustEqual featureGuidesString
    }
  }
}
