package hydro.domain

sealed trait Measurement

object Measurement {

    type Value = Double

    final case class Temperature(value: Value) extends Measurement
    final case class Ec(value: Value) extends Measurement
    final case class Ph(value: Value) extends Measurement

}
