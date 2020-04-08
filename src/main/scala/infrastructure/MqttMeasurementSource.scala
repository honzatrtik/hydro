package infrastructure

import cats.effect._
import cats.implicits._
import domain.{ Measurement, MeasurementSource }
import fs2.Stream
import fs2.concurrent.Queue
import io.circe.parser.parse
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{ IMqttDeliveryToken, MqttCallback, MqttClient, MqttMessage }
import org.http4s.Uri
import wvlet.log.LogSupport

class MqttMeasurementSource(config: MqttMeasurementSource.Config)(implicit cs: ContextShift[IO]) extends MeasurementSource[IO] with LogSupport {

  import MqttMeasurementSource._

  def makeStream: Stream[IO, Measurement] = {
    makeConnection(config.brokerUri)
      .flatMap(registerCallbacks(_, config.topic))
      .collect {
        case Event.MessageArrived(_, body) => {
          parse(body)
            .flatMap(_.as[Measurement](Measurement.Codec.measurementDecoder))
            .some
        }
        case Event.ConnectionLost(_) => none
      }
      .unNoneTerminate
      .collect { case Right(measurement) => measurement }
  }

  private def makeConnection(brokerUri: Uri): Stream[IO, MqttClient] = {
    Stream.bracket(IO {
      val persistence = new MemoryPersistence
      val client = new MqttClient(brokerUri.toString(), MqttClient.generateClientId, persistence)
      client.connect()
      logger.info(s"Connected to ${brokerUri}")
      client
    })(mqttClient => IO(mqttClient.disconnect()))
  }

  private def registerCallbacks(mqttClient: MqttClient, topic: String): Stream[IO, Event] = {
    for {
      _ <- Stream.eval(IO(mqttClient.subscribe(topic)))
      queue <- Stream.eval(Queue.unbounded[IO, Either[Throwable, Event]])
      _ <- Stream.eval(IO.delay {
        mqttClient.setCallback(new MqttCallback {
          def messageArrived(messageTopic: String, message: MqttMessage): Unit = {
            ConcurrentEffect[IO]
              .runAsync(queue.enqueue1(Event.MessageArrived(messageTopic, message.toString).asRight))(_ => {
                IO(logger.debug(s"Message arrived: ${topic} ${message}"))
              })
              .unsafeRunSync()
          }

          def connectionLost(e: Throwable): Unit = {
            ConcurrentEffect[IO]
              .runAsync(queue.enqueue1(Event.ConnectionLost(e).asRight))(_ => {
                IO(logger.debug(s"Connection lost: ${e}"))
              })
              .unsafeRunSync()
          }

          def deliveryComplete(token: IMqttDeliveryToken): Unit = {
            ConcurrentEffect[IO]
              .runAsync(queue.enqueue1(Event.DeliveryComplete().asRight))(_ => {
                IO(logger.debug(s"Delivery complete"))
              })
              .unsafeRunSync()
          }
        })
      })
      event <- queue.dequeue.rethrow
    } yield event
  }

}

object MqttMeasurementSource {

  def apply(config: Config)(implicit cs: ContextShift[IO]): MqttMeasurementSource = new MqttMeasurementSource(config)

  case class Config(brokerUri: Uri, topic: String)

  sealed trait Event
  object Event {
    final case class MessageArrived(topic: String, body: String) extends Event
    final case class ConnectionLost(e: Throwable) extends Event
    final case class DeliveryComplete() extends Event
  }

}


