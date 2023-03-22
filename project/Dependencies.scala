import sbt._

object Dependencies {
  val scalaTest = "3.2.15"
  val uPickle = "2.0.0"
  val autowire = "0.3.3"
  val booPickle = "1.3.3"
  val specs2 = "4.10.6"
  val csvCommons = "1.10.0"
  val catsVersion = "2.9.0"

  val libDeps: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % scalaTest % Test,
    "com.lihaoyi" %% "upickle" % uPickle,
    "com.lihaoyi" %% "autowire" % autowire,
    "io.suzaku" %% "boopickle" % booPickle,
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "org.specs2" %% "specs2-core" % specs2 % Test,
    "org.apache.commons" % "commons-csv" % csvCommons,
    "org.typelevel" %% "cats-core" % catsVersion,
    "com.outr" %% "scribe-slf4j" % "3.11.1"
  )
}
