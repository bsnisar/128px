name := """128px"""
organization := "com.128px"

version := "1.0-SNAPSHOT"


lazy val root = (project in file("."))
  .aggregate(lib)
  .dependsOn(lib)
  .enablePlugins(PlayScala, PlayAkkaHttp2Support)

lazy val lib = project.in(file("lib"))


scalaVersion := "2.13.3"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.parashtash.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.parashtash.binders._"
