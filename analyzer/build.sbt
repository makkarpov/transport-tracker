name := "tracker-analyze"
version := "0.1-SNAPSHOT"
scalaVersion := "2.11.7"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  cache, ws,

  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,

  "com.github.tminglei" %% "slick-pg" % "0.14.2",
  "com.github.tminglei" %% "slick-pg_jts" % "0.14.2",
  "com.github.tminglei" %% "slick-pg_date2" % "0.14.2",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.14.2",

  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"


fork in run := true