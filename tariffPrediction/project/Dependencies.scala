import play.sbt.PlayImport._
import sbt._

object Version {
  val hadoop           = "2.6.0"
  val logback          = "1.1.2"
  val mockito          = "1.10.19"
  val scala            = "2.11.7"
  val scalaTest        = "2.2.1"
  val slf4j            = "1.7.6"
  val spark            = "1.6.1"
  val sparkTestingBase = spark + "0.3.3"
}

object Library {
  val logbackClassic    = "ch.qos.logback"    %  "logback-classic"    % Version.logback
  val mockitoAll        = "org.mockito"       %  "mockito-all"        % Version.mockito
  val scalaTest         = "org.scalatest"     %% "scalatest"          % Version.scalaTest
  val slf4jApi          = "org.slf4j"         %  "slf4j-api"          % Version.slf4j
  val sparkSQL          = "org.apache.spark"  %% "spark-sql"          % Version.spark
  val sparkMLLib        = "org.apache.spark"  %% "spark-mllib"        % Version.spark
  val sparkCore         = "org.apache.spark"  %% "spark-core"         % Version.spark
  val sparkTestingBase  = "com.holdenkarau"   %% "spark-testing-base" % Version.sparkTestingBase
}

object Dependencies {
  import Library._

  val sparkAkkaHadoop = Seq(
    sparkSQL,
    sparkMLLib,
    sparkCore % "provided",
    specs2 % Test,
    logbackClassic % "test",
    scalaTest      % "test",
    "org.scalatestplus" %% "play" % "1.4.0-M3" % "test",
    mockitoAll     % "test",
    "com.vividsolutions" % "jts" % "1.13",
    "org.postgresql" % "postgresql" % "9.4-1205-jdbc42",
    "com.github.tminglei" %% "slick-pg" % "0.9.1",
    "com.typesafe.play" %% "play-slick" % "1.0.1",
    "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1"
  )
}
