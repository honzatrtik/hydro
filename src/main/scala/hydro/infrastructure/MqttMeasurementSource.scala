package hydro.infrastructure

import cats.effect._
import cats.implicits._
import fs2.Stream
import fs2.concurrent.Queue
import hydro.domain.{ Measurement, MeasurementSource }
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{ IMqttDeliveryToken, MqttCallback, MqttClient, MqttMessage }
import org.http4s.Uri
import wvlet.log.LogSupport

class MqttMeasurementSource(config: MqttMeasurementSource.Config)(implicit cs: ContextShift[IO]) extends MeasurementSource[IO] with LogSupport {

  import MqttMeasurementSource._

  def makeStream: Stream[IO, Measurement] = {
    makeConnection(config.brokerUri)
      .flatMap(registerCallbacks(_, config.topicToSubscribe))
      .collect {
        case Event.MessageArrived(topic, body) => {
          config
            .topicValueMapper
            .get(topic)
            .ap(body.toDoubleOption)
            .some

        }
        case Event.ConnectionLost(_) => none
      }
      .unNoneTerminate
      .collect { case Some(measurement) => measurement }
  }

  private def makeConnection(brokerUri: Uri): Stream[IO, MqttClient] = {
    Stream.bracket(IO {
      val persistence = new MemoryPersistence
      val client = new MqttClient(brokerUri.toString(), config.clientId, persistence)
      client.connect()
      logger.info(s"Connected to ${brokerUri}")
      client
    })(mqttClient => IO(mqttClient.disconnect()))
  }

  private def registerCallbacks(mqttClient: MqttClient, topicToSubscribe: Topic): Stream[IO, Event] = {
    for {
      _ <- Stream.eval(IO(mqttClient.subscribe(topicToSubscribe.topic)))
      queue <- Stream.eval(Queue.unbounded[IO, Either[Throwable, Event]])
      _ <- Stream.eval(IO.delay {
        mqttClient.setCallback(new MqttCallback {
          def messageArrived(topic: String, message: MqttMessage): Unit = {
            ConcurrentEffect[IO]
              .runAsync(queue.enqueue1(Event.MessageArrived(Topic(topic), message.toString).asRight))(_ => {
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

  type ValueMapper = Measurement.Value => Measurement

  case class Topic(topic: String)

  case class Config(
    clientId: String,
    brokerUri: Uri,
    topicToSubscribe: Topic,
    topicValueMapper: Map[Topic, ValueMapper]
  )

  object Config {
    def clientIdAndBrokerUriFromEnv(topicToSubscribe: Topic, topicValueMapper: Map[Topic, ValueMapper]): Config = Config(
      sys.env("MQTT_CLIENT_ID"),
      Uri.unsafeFromString(sys.env("MQTT_BROKER_URI")),
      topicToSubscribe,
      topicValueMapper,
    )
  }

  sealed trait Event
  object Event {
    final case class MessageArrived(topic: Topic, body: String) extends Event
    final case class ConnectionLost(e: Throwable) extends Event
    final case class DeliveryComplete() extends Event
  }

}


