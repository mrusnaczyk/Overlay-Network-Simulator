import Dependencies._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "edu.uic.cs.441"
ThisBuild / organizationName := "OverlayNetworkSimulator"

val AkkaVersion = "2.6.10"

lazy val root = (project in file("."))
  .settings(
    name := "OverlayNetworkSimulator",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "org.slf4j" %  "slf4j-api" % "1.7.30",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
  )

