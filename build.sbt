import Dependencies._
import sbt.Keys.libraryDependencies

lazy val scala = "2.13.10"

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
    libraryDependencies ++= libDeps,
    crossScalaVersions := Nil,
    logLevel := Level.Debug,
  )

lazy val akkaVersion = "2.7.0"
lazy val akkaPersistenceInMemoryVersion = "2.5.15.2"
lazy val jodaVersion = "2.10.14"
lazy val upickleVersion = "3.1.0"
lazy val sparkMlLibVersion = "3.3.2"

lazy val cross = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .settings(
    name := "drt-lib",
    libraryDependencies ++= libDeps,
    resolvers ++= Seq(
      "Artifactory Snapshot Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-snapshot/",
      "Artifactory Release Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-release/"
    )
  ).
  jvmSettings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "joda-time" % "joda-time" % jodaVersion,
      "com.lihaoyi" %%% "upickle" % upickleVersion,
      "org.apache.spark" %% "spark-mllib" % sparkMlLibVersion,

      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
      "com.github.dnvriend" %% "akka-persistence-inmemory" % akkaPersistenceInMemoryVersion % "test"
    ),
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value),
    Compile / PB.protoSources := Seq(file("proto/src/main/protobuf")),
    publishTo := Some("release" at artifactory + "artifactory/libs-release")
  ).
  jsSettings(
    publishTo := Some("release" at artifactory + "artifactory/libs-release")
  )
