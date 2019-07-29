import Dependencies._

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
    // Add JVM-specific settings here
  ).
  jsSettings(
    // Add JS-specific settings here
  )

lazy val crossJVM = cross.jvm
lazy val crossJS = cross.js
