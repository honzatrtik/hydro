package domain

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

case class Measurement(temperature: Double, ec: Double, ph: Double)
object Measurement {
  object Implicits {
    lazy val measurementDecoder: Decoder[Measurement] = deriveDecoder[Measurement]
    lazy val measurementEncoder: Encoder[Measurement] = deriveEncoder[Measurement]
  }
}
