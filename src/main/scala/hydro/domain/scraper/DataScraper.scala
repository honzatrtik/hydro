package hydro.domain.scraper

import java.time.ZonedDateTime

import cats.data.Kleisli
import hydro.domain.scraper.DataScraper.{ Environment, ScrapeError, ScrapedData }
import io.circe.Json
import org.http4s.client.Client


trait DataScraper[F[_], C] {
  def scrape: Kleisli[F, Environment[F, C], Either[ScrapeError, ScrapedData]]
}

object DataScraper {
  sealed trait ScrapeError
  object ScrapeError {
    final case class InvalidResponse(description: String) extends ScrapeError
  }

  case class ScrapedData(source: String, dateTime: ZonedDateTime, data: Json)

  case class Environment[F[_], C](client: Client[F], config: C)
}
