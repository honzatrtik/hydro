package hydro

import cats.implicits._
import cats.effect._
import hydro.domain.Measurement.{ Ec, Ph, Temperature }
import hydro.infrastructure.MqttMeasurementSource
import hydro.infrastructure.MqttMeasurementSource.Topic
import org.http4s.Uri
import wvlet.log.{ LogFormatter, LogLevel, LogSupport, Logger }

object Subscriber extends IOApp with LogSupport {

  Logger.setDefaultLogLevel(LogLevel.DEBUG)
  Logger.setDefaultFormatter(LogFormatter.IntelliJLogFormatter)

  def run(args: List[String]): IO[ExitCode] = {

    val mqttConfig = MqttMeasurementSource.Config(
      Uri.unsafeFromString("tcp://test.mosquitto.org"),
      Topic("hydro/1/+"),
      Map(
        Topic("hydro/1/temperature") -> Temperature,
        Topic("hydro/1/ec") -> Ec,
        Topic("hydro/1/ph") -> Ph,
      )
    )

    MqttMeasurementSource(mqttConfig)
      .makeStream
      .compile
      .drain
      .as(ExitCode.Success)
  }

}
