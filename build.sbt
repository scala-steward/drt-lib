import sbt.Keys.libraryDependencies

lazy val scala = "2.13.15"

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

lazy val akkaVersion = "2.7.1"
lazy val akkaHttpVersion = "10.5.3"
lazy val jodaVersion = "2.12.7"
lazy val upickleVersion = "3.1.5"
lazy val sparkMlLibVersion = "3.5.4"
lazy val scalaTestVersion = "3.2.19"
lazy val specs2Version = "4.20.9"
lazy val csvCommonsVersion = "1.13.0"
lazy val catsVersion = "2.12.0"
lazy val scribeSlf4jVersion = "3.16.0"
lazy val slickVersion = "3.4.1"
lazy val h2Version = "2.2.224"
lazy val sprayJsonVersion = "1.3.6"
lazy val sslConfigCoreVersion = "0.6.1"

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
      "Akka library repository".at("https://repo.akka.io/maven"),
      "Artifactory Snapshot Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-snapshot/",
      "Artifactory Release Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-release/"
    )
  ).
  jvmSettings(
    libraryDependencies ++= Seq(
      "com.typesafe" %% "ssl-config-core" % sslConfigCoreVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "joda-time" % "joda-time" % jodaVersion,
      "org.apache.spark" %% "spark-mllib" % sparkMlLibVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
      "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % "test",
      "com.typesafe.slick" %% "slick" % slickVersion,
      "io.spray" %% "spray-json" % sprayJsonVersion,
      "com.h2database" % "h2" % h2Version % Test
    ),
    resolvers ++= Seq(
      "Akka library repository".at("https://repo.akka.io/maven"),
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
  ).
  jsSettings(
    publishTo := Some("release" at artifactory + "artifactory/libs-release")
  )
