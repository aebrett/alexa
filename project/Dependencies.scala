import sbt._
import sbt.Keys._

object Dependencies {

  val dependencies = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7",


    "com.beachape" %% "enumeratum" % "1.5.2",
    "org.scalactic" %% "scalactic" % "3.0.1",

    "com.amazon.alexa" % "alexa-skills-kit" % "1.1.3"
      exclude("log4j", "log4j")
      exclude("org.slf4j", "slf4j-log4j12")
  )

  val settings = Seq(
    libraryDependencies ++= dependencies
  )

}
