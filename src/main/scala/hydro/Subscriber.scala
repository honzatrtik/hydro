package hydro

import cats.implicits._
import cats.effect._
import doobie._
import doobie.implicits._
import doobie.implicits.javatime._
import hydro.domain.Measurement.{ Ec, Ph, Temperature }
import hydro.infrastructure.{ DoobieMeasurementRepository, DoobieTransactor, MqttMeasurementSource }
import hydro.infrastructure.MqttMeasurementSource.Topic
import org.http4s.Uri
import wvlet.log.{ LogFormatter, LogLevel, LogSupport, Logger }

object Subscriber extends IOApp with LogSupport {

  Logger.setDefaultLogLevel(LogLevel.DEBUG)
  Logger.setDefaultFormatter(LogFormatter.IntelliJLogFormatter)

  def run(args: List[String]): IO[ExitCode] = {

    val doobieConfig = DoobieTransactor.Config(
      "org.postgresql.Driver",
      "jdbc:postgresql://127.0.0.1/hydro",
      "hydro",
      "hydr0hydrO",
    )

    val mqttConfig = MqttMeasurementSource.Config(
      Uri.unsafeFromString("tcp://test.mosquitto.org"),
      Topic("hydro/1/+"),
      Map(
        Topic("hydro/1/temperature") -> Temperature,
        Topic("hydro/1/ec") -> Ec,
        Topic("hydro/1/ph") -> Ph,
      )
    )

    val clock = Clock.create[IO]
    val transactor = DoobieTransactor
      .make(doobieConfig)

    val repository = new DoobieMeasurementRepository(transactor, clock)

    MqttMeasurementSource(mqttConfig)
      .makeStream
      .evalMapChunk(repository.save)
      .compile
      .drain
      .as(ExitCode.Success)
  }

}
