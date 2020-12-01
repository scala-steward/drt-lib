import sbt._

object Dependencies {
  val scalaTest = "3.0.8"
  val autowire = "0.2.6"
  val uPickle = "0.6.7"
  val booPickle = "1.2.6"
  val specs2 = "4.10.0"

  val libDeps = Seq(
    "org.scalatest" %% "scalatest" % "3.0.8",
    "com.lihaoyi" %% "autowire" % autowire,
    "com.lihaoyi" %% "upickle" % uPickle,
    "io.suzaku" %% "boopickle" % booPickle,
    "org.specs2" %% "specs2-core" % specs2 % "test"
  )
}
