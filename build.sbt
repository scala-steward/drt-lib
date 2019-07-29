import Dependencies._

lazy val scala212 = "2.12.8"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / scalaVersion := "2.11.12"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "uk.gov.homeoffice"
ThisBuild / organizationName := "drt"


lazy val root = project.in(file(".")).
  aggregate(crossJS, crossJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

lazy val cross = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(
    name := "drt-lib",
    libraryDependencies ++= libDeps
  ).
  jvmSettings(
    crossScalaVersions := supportedScalaVersions
  ).
  jsSettings(
    crossScalaVersions := supportedScalaVersions
  )

lazy val crossJVM = cross.jvm
lazy val crossJS = cross.js

publishTo := {
  val artifactory = "https://artifactory.digital.homeoffice.gov.uk/"

  Some("release" at artifactory + "artifactory/libs-release")
}
