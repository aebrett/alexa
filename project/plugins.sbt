resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"

// upgrade sbt-git's jgit dependency to avoid logged IOExceptions in windows (and
// avoid an eviction warning when we do so)
libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.5.0.201609210915-r"
evictionWarningOptions in update :=
  EvictionWarningOptions.default.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)

// prevent warning about missing StaticLoggerBinder being logged to the console when running sbt-git
// tasks (since they try to log using slf4j)
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.22"

addSbtPlugin("com.gilt.sbt" % "sbt-aws-lambda" % "0.4.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
