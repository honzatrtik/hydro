package hydro.infrastructure.scraper

import cats.data.{ Kleisli, NonEmptyList }
import cats.effect.IO
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.implicits.javatime._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.postgres.pgisimplicits._
import doobie.util.transactor.Transactor
import hydro.domain.scraper.DataScraper.ScrapedData
import io.circe.Json
import org.postgresql.util.PGobject

object DoobieScraperRepository {

  implicit val jsonPut: Put[Json] =
    Put.Advanced.other[PGobject](NonEmptyList.of("json")).tcontramap[Json] { j =>
      val o = new PGobject
      o.setType("json")
      o.setValue(j.noSpaces)
      o
    }

  def save(data: ScrapedData): Kleisli[IO, Transactor[IO], Unit] = Kleisli { transactor =>
    sql"insert into scraper_data (timestamp, source, data) values (${data.dateTime.toOffsetDateTime}, ${data.source}, ${data.data})"
      .update
      .run
      .transact(transactor)
      .void
  }
}