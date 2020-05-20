ThisBuild / scalaVersion := "2.13.1"

val doobieVersion = "0.9.0"
val doobie = Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-hikari",
  "org.tpolecat" %% "doobie-postgres",
).map(_ % doobieVersion)

val http4sVersion = "0.21.4"
val http4s = Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
)

val circeVersion = "0.12.3"
val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

lazy val root = (project in file("."))
  .settings(
    name := "hydro",
    resolvers ++= Seq(
      "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/",
    ),
    libraryDependencies ++= Seq(
      "org.eclipse.paho" % "mqtt-client" % "0.4.0",
      "org.wvlet.airframe" %% "airframe-log" % "20.4.0",
      "org.slf4j" % "slf4j-jdk14" % "1.7.21",
    ) ++ doobie ++ http4s ++ circe,
    packageName in Docker := "hydro/app",
    dockerBaseImage := "openjdk:8-jre",
    dockerRepository := Some("docker.pkg.github.com"),
    dockerUsername := Some("honzatrtik"),
  )

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)