ThisBuild / scalaVersion := "2.13.1"

val doobieVersion = "0.9.0"
val doobie = Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-hikari",
  "org.tpolecat" %% "doobie-postgres",
).map(_ % doobieVersion)

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
    ) ++ doobie,
    packageName in Docker := "hydro/app",
    dockerBaseImage := "openjdk:8-jdk-alpine3.9",
    dockerRepository := Some("docker.pkg.github.com"),
    dockerUsername := Some("honzatrtik"),
  )

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)