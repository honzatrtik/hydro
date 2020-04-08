package hydro.domain

trait MeasurementRepository[F[_]] {
  def save(measurement: Measurement): F[Unit]
}
