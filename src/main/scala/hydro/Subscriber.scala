package hydro

import cats.effect._
import hydro.domain.Measurement.{ Ec, Ph, Temperature }
import hydro.domain.{ MeasurementRepository, MeasurementSource, Program }
import hydro.infrastructure.MqttMeasurementSource.Topic
import hydro.infrastructure.{ DoobieMeasurementRepository, DoobieTransactor, LoggerConfig, MqttMeasurementSource }
import wvlet.log.{ LogSupport, Logger }

object Subscriber extends IOApp with LogSupport {

  private val loggerConfig = LoggerConfig.default

  Logger.setDefaultLogLevel(loggerConfig.level)
  Logger.setDefaultFormatter(loggerConfig.formatter)

  private val doobieConfig = DoobieTransactor.Config.fromEnv

  private val mqttConfig = MqttMeasurementSource.Config.clientIdAndBrokerUriFromEnv(
    Topic("hydro/1/+"),
    Map(
      Topic("hydro/1/temperature") -> Temperature,
      Topic("hydro/1/ec") -> Ec,
      Topic("hydro/1/ph") -> Ph,
    )
  )

  def run(args: List[String]): IO[ExitCode] = {
    val clock = Clock.create[IO]
    val transactor = DoobieTransactor
      .make(doobieConfig)

    Program[IO].run(new Program.Context[IO] {
      def measurementRepository: MeasurementRepository[IO] = new DoobieMeasurementRepository(transactor, clock)
      def measurementSource: MeasurementSource[IO] = MqttMeasurementSource(mqttConfig)
    })
  }

}
