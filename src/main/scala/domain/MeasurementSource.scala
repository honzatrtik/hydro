package domain

import fs2._

trait MeasurementSource[F[_]] {
  def makeStream: Stream[F, Measurement]
}

object MeasurementSource {
}
