name := "cinema"

version := "0.1"

scalaVersion := "2.13.5"

val logbackVersion      = "1.2.3"
val scalaLoggingVersion = "3.9.2"
val AkkaVersion = "2.6.13"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,

  "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,

  "ch.qos.logback"              % "logback-classic" % logbackVersion,
  "com.typesafe.scala-logging" %% "scala-logging"   % scalaLoggingVersion,

  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
)
