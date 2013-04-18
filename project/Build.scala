import sbt._
import Keys._

object ScrapperBuild extends Build {

  import Dependencies._

  var unmanagedListing = unmanagedJars in Compile :=  {
    Dependencies.listUnmanaged( file(".").getAbsoluteFile )
  }

  lazy val defaultSettings = Seq(
    organization := "com.geekple"
    , version := "0.1"
    , resolvers ++= resolutionRepos
    , libraryDependencies ++= Seq(akkaActor, akkaSlf4j, sprayCan, sprayRouting, sprayJson)
    , scalaVersion := "2.10.1"
    , scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")
    , unmanagedListing
  )

  lazy val root = Project("root", file("."))
    .aggregate(common, server, client)

  lazy val common = Project("common", file("common"))
    .settings(defaultSettings: _*)
    .settings(libraryDependencies ++= Seq(metricsClient))

  lazy val server = Project("server", file("server"))
    .dependsOn(common)
    .settings(defaultSettings: _*)
    .settings(libraryDependencies ++= Seq(rome, redis, metricsJson, metricsServer, jackson))

  lazy val client = Project("client", file("client"))
    .dependsOn(common)

}
