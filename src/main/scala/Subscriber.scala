import cats.effect._
import cats.implicits._
import domain.Measurement
import fs2.Stream
import fs2.concurrent.Queue
import io.circe.parser._
import io.circe.syntax._
import org.eclipse.paho.client.mqttv3._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.http4s.Uri
import wvlet.log.{ LogFormatter, LogLevel, Logger }

object Subscriber extends IOApp {

  private val logger = Logger.of[Subscriber.type]
  logger.setFormatter(LogFormatter.IntelliJLogFormatter)
  logger.setLogLevel(LogLevel.DEBUG)

  sealed trait Event
  final case class MessageArrived(
    topic: String,
    body: String
  ) extends Event
  final case class ConnectionLost(e: Throwable) extends Event
  final case class DeliveryComplete() extends Event

  def run(args: List[String]): IO[ExitCode] = {

    val brokerUrl = "tcp://192.168.1.100:1883"
    val topic = "outTopic"

    makeConnection(Uri.unsafeFromString(brokerUrl))
      .flatMap(makeEventStream(_, topic))
      .collect {
        case MessageArrived(_, body) => parse(body).flatMap(_.as[Measurement](Measurement.Implicits.measurementDecoder)).some
        case ConnectionLost(e) => none
      }
      .unNoneTerminate
      .flatMap {
        case Right(measurement) => Stream.eval(IO(logger.debug(measurement.asJson(Measurement.Implicits.measurementEncoder).noSpaces)))
        case Left(e) => Stream.eval(IO(logger.warn(s"Decoding failed ${e.getMessage}")))
      }
      .compile
      .drain
      .as(ExitCode.Success)

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

  private def makeEventStream(mqttClient: MqttClient, topic: String): Stream[IO, Event] = {
    for {
      _ <- Stream.eval(IO(mqttClient.subscribe(topic)))
      queue <- Stream.eval(Queue.unbounded[IO, Either[Throwable, Event]])
      _ <- Stream.eval(IO.delay {
        mqttClient.setCallback(new MqttCallback {
          def messageArrived(messageTopic: String, message: MqttMessage): Unit = {
            ConcurrentEffect[IO]
              .runAsync(queue.enqueue1(MessageArrived(messageTopic, message.toString).asRight))(_ => {
                IO(logger.debug(s"Message arrived: ${topic} ${message}"))
              })
              .unsafeRunSync()
          }

          def connectionLost(e: Throwable): Unit = {
            ConcurrentEffect[IO]
              .runAsync(queue.enqueue1(ConnectionLost(e).asRight))(_ => {
                IO(logger.debug(s"Connection lost: ${e}"))
              })
              .unsafeRunSync()
          }

          def deliveryComplete(token: IMqttDeliveryToken): Unit = {
            ConcurrentEffect[IO]
              .runAsync(queue.enqueue1(DeliveryComplete().asRight))(_ => {
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
