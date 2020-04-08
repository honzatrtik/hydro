ThisBuild / scalaVersion := "2.13.1"

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
      "org.typelevel" %% "cats-effect" % "2.1.2",
      "org.http4s" %% "http4s-core" % "0.21.3",
      "org.wvlet.airframe" %% "airframe-log" % "20.4.0",
    ) ++ circe,
  )