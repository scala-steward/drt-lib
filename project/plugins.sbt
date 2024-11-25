ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" % "scala-xml" % VersionScheme.Always
)

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.14.0")

addSbtPlugin("net.vonbuchholtz" %% "sbt-dependency-check" % "5.1.0")
addDependencyTreePlugin

addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.2.8")
