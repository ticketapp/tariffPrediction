name := """Crawler"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
lazy val akkaVersion = "2.4.4"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  evolutions,
  filters,
  specs2 % Test,
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  "de.leanovate.play-mockws" %% "play-mockws" % "2.5.0" % "test",
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1",
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "com.vividsolutions" % "jts" % "1.13",
  "com.lihaoyi" %% "sourcecode" % "0.1.0",
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

javaOptions in Test += "-Dlogger.resource=logback-test.xml"
