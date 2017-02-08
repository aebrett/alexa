import sbt._
import sbt.Keys._

object Common {

  lazy val settings = Seq(
    name := "Alexa",
    organization := "org.bretts",
    version := "0.1-SNAPSHOT",

    scalaVersion := "2.12.1",

    scalacOptions := Seq(
      "-target:jvm-1.8",
      "-Xfuture", "-Xexperimental",
      "-Xcheckinit",
      "-Xfatal-warnings",
      "-deprecation", "-unchecked", "-feature",
      "-Xlint:_",
      "-Ywarn-adapted-args", "-Ywarn-dead-code", "-Ywarn-inaccessible", "-Ywarn-infer-any",
      "-Ywarn-nullary-override", "-Ywarn-nullary-unit", "-Ywarn-unused", "-Ywarn-unused-import"
      // "-Ywarn-numeric-widen", "-Ywarn-value-discard" <-- overly stringent warnings
    )
  )
}
