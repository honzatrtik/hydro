package hydro.domain

sealed trait Measurement {
  def name: String
  def value: Measurement.Value
}

object Measurement {

  type Value = Double

  final case class Temperature(value: Value) extends Measurement {
    val name = "temperature"
  }
  final case class Ec(value: Value) extends Measurement {
    val name = "ec"
  }
  final case class Ph(value: Value) extends Measurement {
    val name = "ph"
  }

}
