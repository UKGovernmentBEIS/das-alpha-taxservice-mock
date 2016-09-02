name := "das-alpha-taxservice-mock"

lazy val `das-alpha-taxservice-mock` = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)

  .enablePlugins(GitVersioning)
  .enablePlugins(GitBranchPrompt)

git.useGitDescribe := true

scalaVersion := "2.11.8"

PlayKeys.devSettings := Seq("play.server.http.port" -> "9002")

resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies ++= Seq(
  ws,
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.17.0",
  "uk.gov.hmrc" %% "domain" % "3.5.0",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.typelevel" %% "cats-core" % "0.7.0",
  "com.github.melrief" %% "pureconfig" % "0.1.6",
  "org.reactivemongo" %% "reactivemongo" % "0.11.14",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test
)
