ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" % "scala-xml" % VersionScheme.Always
)

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.2")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.20.2")

addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.2.13")

addSbtPlugin("net.nmoncho" % "sbt-dependency-check" % "1.7.2")
