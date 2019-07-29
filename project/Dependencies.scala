import sbt._

object Dependencies {
  val scalaTest = "3.0.8"
  val autowire = "0.2.6"
  val uPickle = "0.6.7"
  val booPickle = "1.2.6"

  val libDeps = Seq(
    "org.scalatest" %% "scalatest" % "3.0.8",
    "com.lihaoyi" %% "autowire" % autowire,
    "com.lihaoyi" %% "upickle" % uPickle,
    "io.suzaku" %% "boopickle" % booPickle
  )
}
