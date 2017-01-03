// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.10")

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")


resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.7")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15-1")

