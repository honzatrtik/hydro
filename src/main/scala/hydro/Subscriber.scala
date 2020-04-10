package hydro

import cats.Applicative
import cats.data.Kleisli
import cats.implicits._
import cats.effect._
import fs2.Stream
import hydro.domain.Measurement.{ Ec, Ph, Temperature }
import hydro.domain.{ MeasurementRepository, MeasurementSource }
import hydro.infrastructure.{ DoobieMeasurementRepository, DoobieTransactor, MqttMeasurementSource }
import hydro.infrastructure.MqttMeasurementSource.Topic
import org.http4s.Uri
import wvlet.log.{ LogFormatter, LogLevel, LogSupport, Logger }

object Subscriber extends IOApp with LogSupport {

  Logger.setDefaultLogLevel(LogLevel.DEBUG)
  Logger.setDefaultFormatter(LogFormatter.IntelliJLogFormatter)

  private val doobieConfig = DoobieTransactor.Config(
    "org.postgresql.Driver",
    "jdbc:postgresql://127.0.0.1/hydro",
    "hydro",
    "hydr0hydrO",
  )

  private val mqttConfig = MqttMeasurementSource.Config(
    Uri.unsafeFromString("tcp://test.mosquitto.org"),
    Topic("hydro/1/+"),
    Map(
      Topic("hydro/1/temperature") -> Temperature,
      Topic("hydro/1/ec") -> Ec,
      Topic("hydro/1/ph") -> Ph,
    )
  )

  trait Context[F[_]] {
    def measurementRepository: MeasurementRepository[F]
    def measurementSource: MeasurementSource[F]
  }

  private def program[F[_]: Applicative](implicit compiler: Stream.Compiler[F, F]):  Kleisli[F, Context[F], ExitCode] = {
    Kleisli[F, Context[F], ExitCode] { context =>
      context.measurementSource
        .makeStream
        .evalMapChunk(context.measurementRepository.save)
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }

  def run(args: List[String]): IO[ExitCode] = {
    val clock = Clock.create[IO]
    val transactor = DoobieTransactor
      .make(doobieConfig)

    program[IO].run(new Context[IO] {
      def measurementRepository: MeasurementRepository[IO] = new DoobieMeasurementRepository(transactor, clock)
      def measurementSource: MeasurementSource[IO] = MqttMeasurementSource(mqttConfig)
    })
  }

}
