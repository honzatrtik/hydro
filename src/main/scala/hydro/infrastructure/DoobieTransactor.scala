package hydro.infrastructure

import cats.effect.{ ContextShift, IO }
import doobie.util.transactor.Transactor

object DoobieTransactor {

  case class Config(
    driver: String,
    url: String,
    user: String,
    password: String,
  )

  def make(config: Config)(implicit cs: ContextShift[IO]): Transactor[IO] = {
    Transactor.fromDriverManager[IO](
      config.driver,
      config.url,
      config.user,
      config.password,
    )
  }
}
