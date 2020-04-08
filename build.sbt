ThisBuild / scalaVersion := "2.13.1"


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
    ),
  )