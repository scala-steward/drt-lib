import Dependencies._

lazy val scala = "2.12.15"

ThisBuild / scalaVersion := scala
ThisBuild / organization := "uk.gov.homeoffice"
ThisBuild / organizationName := "drt"
ThisBuild / version := "v" + sys.env.getOrElse("DRONE_BUILD_NUMBER", sys.env.getOrElse("BUILD_ID", "DEV"))

val artifactory = "https://artifactory.digital.homeoffice.gov.uk/"

lazy val root = project.in(file(".")).
  aggregate(crossJS, crossJVM).
  settings(
    name := "drt-lib",
    publish := {},
    publishLocal := {},
    libraryDependencies ++= libDeps,
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceDirectory).value / "scala"
    ),
    PB.deleteTargetDirectory := false
  )

lazy val cross = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(
    name := "drt-lib",
    libraryDependencies ++= libDeps
  ).
  jvmSettings(
    publishTo := Some("release" at artifactory + "artifactory/libs-release")
  ).
  jsSettings(
    publishTo := Some("release" at artifactory + "artifactory/libs-release")
  )

lazy val crossJVM = cross.jvm
lazy val crossJS = cross.js
