package hydro

import cats.data.EitherT
import cats.effect._
import cats.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream
import hydro.domain.scraper.DataScraper
import hydro.infrastructure.scraper.{ DoobieScraperRepository, OpenWeatherMap, OpenWeatherMapConfig }
import hydro.infrastructure.{ DoobieTransactor, LoggerConfig }
import org.http4s.client.blaze._
import wvlet.log.{ LogSupport, Logger }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Scraper extends IOApp with LogSupport {

  private val loggerConfig = LoggerConfig.default

  Logger.setDefaultLogLevel(loggerConfig.level)
  Logger.setDefaultFormatter(loggerConfig.formatter)

  private val openWeatherMapConfig = OpenWeatherMapConfig.fromEnv
  private val doobieConfig = DoobieTransactor.Config.fromEnv

  def run(args: List[String]): IO[ExitCode] = {

    case class AppConfig(
      dataScraperEnvironment: DataScraper.Environment[IO, OpenWeatherMapConfig],
      transactor: Transactor[IO],
    )

    case class AppError(description: String)

    BlazeClientBuilder[IO](global).resource.use { client =>
      val appConfig = AppConfig(
        DataScraper.Environment(client, openWeatherMapConfig),
        DoobieTransactor.make(doobieConfig)
      )

      Stream.awakeEvery[IO](1.minute)
        .flatMap { _ =>
          Stream.eval_(
            EitherT {
              OpenWeatherMap
                .scrape
                .local((_: AppConfig).dataScraperEnvironment)
            }
              .leftMap(_ => AppError("Scrape error"))
              .flatMap { data =>
                EitherT.right[AppError] {
                  DoobieScraperRepository
                    .save(data)
                    .local((_: AppConfig).transactor)
                }
              }
              .value
              .run(appConfig)
          )
        }
        .compile
        .drain
        .as(ExitCode.Success)
    }

  }

}
