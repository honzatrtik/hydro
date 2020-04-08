package hydro

import cats.implicits._
import cats.effect._
import hydro.infrastructure.MqttMeasurementSource
import org.http4s.Uri
import wvlet.log.{ LogFormatter, LogLevel, LogSupport, Logger }

object Subscriber extends IOApp with LogSupport {

  Logger.setDefaultLogLevel(LogLevel.DEBUG)
  Logger.setDefaultFormatter(LogFormatter.IntelliJLogFormatter)

  def run(args: List[String]): IO[ExitCode] = {

    val mqttConfig = MqttMeasurementSource.Config(
      Uri.unsafeFromString("tcp://test.mosquitto.org"),
      "outTopic",
    )

    MqttMeasurementSource(mqttConfig)
      .makeStream
      .compile
      .drain
      .as(ExitCode.Success)
  }

}
