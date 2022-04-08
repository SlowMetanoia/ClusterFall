name := "ClusterFall"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.8"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
