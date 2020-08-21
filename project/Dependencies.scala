import sbt._

object Dependencies {
  val scalaTest = "3.0.8"
  val uPickle = "1.2.0"
  val autowire = "0.2.6"
  val booPickle = "1.2.6"
  val specs2 = "4.10.0"

  val libDeps = Seq(
    "org.scalatest" %% "scalatest" % "3.0.8",
    "com.lihaoyi" %% "upickle" % uPickle,
    "com.lihaoyi" %% "autowire" % autowire,
    "io.suzaku" %% "boopickle" % booPickle,
    "org.specs2" %% "specs2-core" % specs2 % "test"
  )
}
