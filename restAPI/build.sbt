name := """rest API"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
lazy val akkaVersion = "2.4.4"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  specs2 % Test,
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.postgresql" % "postgresql" % "9.4-1205-jdbc42",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1",
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
  "com.github.tminglei" %% "slick-pg" % "0.9.1",
  "com.vividsolutions" % "jts" % "1.13",
  "com.zaxxer" % "HikariCP" % "2.4.1",
  "com.lihaoyi" %% "sourcecode" % "0.1.0",
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
