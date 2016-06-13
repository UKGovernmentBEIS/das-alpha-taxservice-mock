name := "das-alpha-taxservice-mock"

enablePlugins(PlayScala)
disablePlugins(PlayLayoutPlugin)

enablePlugins(GitVersioning)
enablePlugins(GitBranchPrompt)

git.useGitDescribe := true

scalaVersion := "2.11.8"

PlayKeys.devSettings := Seq("play.server.http.port" -> "9002")

resolvers += Resolver.bintrayRepo("hmrc", "releases")
resolvers += "Madoushi sbt-plugins" at "https://dl.bintray.com/madoushi/sbt-plugins/"

libraryDependencies ++= Seq(
  cache,
  ws,
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.17.0",
  "uk.gov.hmrc" %% "domain" % "3.5.0",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.h2database" % "h2" % "1.4.191",
  "org.postgresql" % "postgresql" % "9.4.1208",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.typelevel" %% "cats" % "0.4.0",
  "com.github.melrief" %% "pureconfig" % "0.1.6",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test
)
