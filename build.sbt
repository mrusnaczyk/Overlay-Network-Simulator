ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "edu.uic.cs.441"
ThisBuild / organizationName := "OverlayNetworkSimulator"

val AkkaVersion = "2.6.10"
val AkkaManagementVersion = "2.6.10"
val AkkaHttpVersion = "10.2.1"


lazy val root = (project in file("."))
  .settings(
    name := "OverlayNetworkSimulator",
    libraryDependencies += "org.scala-lang" % "scala-library" % "2.13",
    libraryDependencies += "io.circe" %% "circe-core" % "0.12.3",
    libraryDependencies += "io.circe" %% "circe-generic" % "0.12.3",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.12.3",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.3" % Test,
    libraryDependencies += "io.circe" %% "circe-yaml" % "0.13.1",
    libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "org.slf4j" %  "slf4j-api" % "1.7.30",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)
