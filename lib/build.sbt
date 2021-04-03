name := "vespa-backend"

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
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "2.0.2"
libraryDependencies +=   "com.lightbend.akka" %% "akka-stream-alpakka-file" % "2.0.2"
libraryDependencies += "org.julienrf" %% "play-json-derived-codecs" % "9.0.0"
libraryDependencies += "org.imgscalr" % "imgscalr-lib" % "4.2"
libraryDependencies += "net.coobird" % "thumbnailator" % "0.4.3"

