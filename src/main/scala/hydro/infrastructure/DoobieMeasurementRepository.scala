package hydro.infrastructure

import java.time.{ Instant, ZoneId }

import cats.effect.{ Clock, IO }
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.implicits.javatime._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.postgres.pgisimplicits._
import doobie.util.transactor.Transactor
import hydro.domain.{ Measurement, MeasurementRepository }

import scala.concurrent.duration.MILLISECONDS

class DoobieMeasurementRepository(transactor: Transactor[IO], clock: Clock[IO]) extends MeasurementRepository[IO] {
  def save(measurement: Measurement): IO[Unit] = {
    clock.realTime(MILLISECONDS)
      .flatMap { epoch =>
        val dateTime = Instant.ofEpochMilli(epoch)
          .atZone(ZoneId.systemDefault)
          .toLocalDateTime

        sql"insert into measurements (timestamp, name, value) values (${dateTime}, ${measurement.name}, ${measurement.value})"
          .update
          .run
          .transact(transactor)
          .void
      }
  }
}