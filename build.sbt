import net.nmoncho.sbt.dependencycheck.settings.AnalyzerSettings
import sbt.Keys.libraryDependencies

lazy val scala = "2.13.16"

ThisBuild / scalaVersion := scala
ThisBuild / organization := "uk.gov.homeoffice"
ThisBuild / organizationName := "drt"
ThisBuild / version := "v" + sys.env.getOrElse("DRONE_BUILD_NUMBER", sys.env.getOrElse("BUILD_ID", "DEV"))

val artifactory = "https://artifactory.digital.homeoffice.gov.uk/"

lazy val root = project.in(file(".")).
  aggregate(cross.js, cross.jvm).
  settings(
    name := "drt-lib",
    publish := {},
    publishLocal := {},
    crossScalaVersions := Nil,
    logLevel := Level.Debug
  )

lazy val pekkoVersion = "1.1.3"
lazy val pekkoHttpVersion = "1.2.0"
lazy val slickVersion = "3.5.2"

lazy val jodaVersion = "2.12.7"
lazy val upickleVersion = "3.1.5"
lazy val sparkMlLibVersion = "3.5.7"
lazy val scalaTestVersion = "3.2.19"
lazy val specs2Version = "4.23.0"
lazy val csvCommonsVersion = "1.13.0"
lazy val catsVersion = "2.12.0"
lazy val scribeSlf4jVersion = "3.16.0"
lazy val h2Version = "2.4.240"


lazy val cross = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .settings(
    name := "drt-lib",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "com.lihaoyi" %% "upickle" % upickleVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.apache.commons" % "commons-csv" % csvCommonsVersion,
      "org.typelevel" %% "cats-core" % catsVersion,
      "com.outr" %% "scribe-slf4j" % scribeSlf4jVersion
    ),
    resolvers ++= Seq(
      "Artifactory Snapshot Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-snapshot/",
      "Artifactory Release Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-release/"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor" % pekkoVersion,
      "org.apache.pekko" %% "pekko-persistence" % pekkoVersion,
      "org.apache.pekko" %% "pekko-persistence-query" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
      "joda-time" % "joda-time" % jodaVersion,
      "org.apache.spark" %% "spark-mllib" % sparkMlLibVersion,
      "org.apache.pekko" %% "pekko-testkit" % pekkoVersion % "test",
      "org.apache.pekko" %% "pekko-persistence-testkit" % pekkoVersion % "test",
      "com.typesafe.slick" %% "slick" % slickVersion,
      "com.h2database" % "h2" % h2Version % Test
    ),
    ThisBuild / dependencyCheckAnalyzers := dependencyCheckAnalyzers.value.copy(
      ossIndex = AnalyzerSettings.OssIndex(
        enabled = Some(false),
        url = None,
        batchSize = None,
        requestDelay = None,
        useCache = None,
        warnOnlyOnRemoteErrors = None,
        username = None,
        password = None
      )
    ),
    Test / parallelExecution := false,
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value),
    Compile / PB.protoSources := Seq(file("proto/src/main/protobuf")),
    Compile / PB.protocExecutable := {
      val osName = System.getProperty("os.name").toLowerCase
      if (osName.contains("mac"))
        file("/opt/homebrew/bin/protoc")
      else
        file("/usr/bin/protoc")
    },
    publishTo := Some("release" at artifactory + "artifactory/libs-release")
  )
  .jsSettings(
    publishTo := Some("release" at artifactory + "artifactory/libs-release")
  )
