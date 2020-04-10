package hydro.domain

import cats.implicits._
import cats.Applicative
import cats.data.Kleisli
import cats.effect.ExitCode
import fs2.Stream

object Program {

  trait Context[F[_]] {
    def measurementRepository: MeasurementRepository[F]
    def measurementSource: MeasurementSource[F]
  }

  def apply[F[_]: Applicative](implicit compiler: Stream.Compiler[F, F]):  Kleisli[F, Context[F], ExitCode] = {
    Kleisli[F, Context[F], ExitCode] { context =>
      context.measurementSource
        .makeStream
        .evalMapChunk(context.measurementRepository.save)
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }

}
