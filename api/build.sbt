name := "ml-api"

version := "BUILD"
scalaVersion := "2.13.3"

// MANAGED DEPENDENCIES
// Each library below is automatically downloaded from one of the resolvers defined in sbt
// See http://www.scala-sbt.org/0.13.2/docs/Getting-Started/Library-Dependencies.html#the-librarydependencies-key
// Libraries in this project are available to all projects
libraryDependencies ++= Seq(
  guice,
  ws,
  logback
)
libraryDependencies += "ai.djl" % "api" % "0.10.0"
libraryDependencies += "ai.djl.pytorch" % "pytorch-model-zoo" % "0.10.0"

libraryDependencies += "ai.djl.pytorch" % "pytorch-engine" % "0.10.0" % "runtime"
libraryDependencies += "ai.djl.pytorch" % "pytorch-native-auto" % "1.7.1" % "runtime"
//libraryDependencies += "ai.djl.pytorch" % "pytorch-native-cpu" % "1.7.1" % "runtime" classifier "osx-x86_64"

libraryDependencies += specs2 % Test
