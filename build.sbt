import Dependencies._
import sbt.Keys.libraryDependencies

lazy val scala = "2.12.15"

lazy val scala212 = "2.12.15"
lazy val scala213 = "2.13.8"
lazy val supportedScalaVersions = List(scala212, scala213)

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
    crossScalaVersions := Nil
  )

lazy val cross = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .settings(
    name := "drt-lib",
    libraryDependencies ++= libDeps
  ).
  jvmSettings(
    publishTo := Some("release" at artifactory + "artifactory/libs-release"),
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value),
    Compile / PB.protoSources := Seq(file("proto/src/main/protobuf")),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.6.17"
    ),
    crossScalaVersions := supportedScalaVersions
  ).
  jsSettings(
    publishTo := Some("release" at artifactory + "artifactory/libs-release"),
    crossScalaVersions := supportedScalaVersions
  )
