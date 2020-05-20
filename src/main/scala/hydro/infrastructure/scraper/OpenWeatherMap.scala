package hydro.infrastructure.scraper

import java.time.Instant
import java.util.TimeZone

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import hydro.domain.scraper.DataScraper
import hydro.domain.scraper.DataScraper.{ ScrapeError, ScrapedData }
import io.circe.Json
import org.http4s.circe._

object OpenWeatherMap extends DataScraper[IO, OpenWeatherMapConfig] {

  /** @see https://openweathermap.org/current */
  def scrape: Kleisli[IO, DataScraper.Environment[IO, OpenWeatherMapConfig], Either[DataScraper.ScrapeError, ScrapedData]] = {
    Kleisli {
      case DataScraper.Environment(client, config) =>
        client
          .expect[Json](s"http://api.openweathermap.org/data/2.5/weather?id=${config.cityId}&appid=${config.apiKey}&units=metric")
          .map { json =>
            json
              .hcursor
              .downField("dt")
              .as[Long]
              .map { epoch =>
                val dateTime = Instant.ofEpochMilli(epoch).atZone(TimeZone.getTimeZone("UTC").toZoneId)
                ScrapedData("openweathermap.org", dateTime, json)
              }
              .leftMap(decodingFailure => ScrapeError.InvalidResponse(decodingFailure.message))
          }

    }
  }
}

case class OpenWeatherMapConfig(apiKey: String, cityId: Long)
object OpenWeatherMapConfig {
  def fromEnv: OpenWeatherMapConfig = {
    OpenWeatherMapConfig(
      sys.env("OPEN_WEATHER_MAP_API_KEY"),
      sys.env("OPEN_WEATHER_MAP_CITY_ID").toLong,
    )
  }
}
