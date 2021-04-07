name := """128px"""
organization := "com.128px"

version := "1.0-SNAPSHOT"


lazy val root = (project in file("."))
  .aggregate(lib, api)
  .dependsOn(lib, api)
  .enablePlugins(PlayScala, PlayAkkaHttp2Support)

lazy val lib = project.in(file("lib"))
  .enablePlugins(ProtobufPlugin)

lazy val api = (project in file("api"))

scalaVersion := "2.13.3"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

libraryDependencies += "ai.djl.pytorch" % "pytorch-engine" % "0.10.0" % "runtime"
libraryDependencies += "ai.djl.pytorch" % "pytorch-native-auto" % "1.7.1" % "runtime"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.parashtash.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.parashtash.binders._"
