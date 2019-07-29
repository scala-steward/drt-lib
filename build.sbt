import Dependencies._

lazy val scala212 = "2.12.8"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / scalaVersion := "2.11.12"
ThisBuild / organization := "uk.gov.homeoffice"
ThisBuild / organizationName := "drt"
ThisBuild / version := "0.2.0"

val artifactory = "https://artifactory.digital.homeoffice.gov.uk/"

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
    crossScalaVersions := supportedScalaVersions,
    publishTo := Some("release" at artifactory + "artifactory/libs-release")
  ).
  jsSettings(
    crossScalaVersions := supportedScalaVersions,
    publishTo := Some("release" at artifactory + "artifactory/libs-release")
  )

lazy val crossJVM = cross.jvm
lazy val crossJS = cross.js


