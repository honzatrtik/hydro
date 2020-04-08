import cats.implicits._
import cats.effect._
import fs2.Stream
import fs2.concurrent.Queue
import org.eclipse.paho.client.mqttv3._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.http4s.Uri

object Subscriber extends IOApp {

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
      .compile
      .drain
      .as(ExitCode.Success)

  }

  private def makeConnection(brokerUrl: Uri): Stream[IO, MqttClient] = {
    Stream.bracket(IO {
      val persistence = new MemoryPersistence
      val client = new MqttClient(brokerUrl.toString(), MqttClient.generateClientId, persistence)
      client.connect()
      println("Connected")
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
                IO(println(s"Message arrived: ${topic} ${message}"))
              })
              .unsafeRunSync()
          }

          def connectionLost(e: Throwable): Unit = {
            ConcurrentEffect[IO]
              .runAsync(queue.enqueue1(ConnectionLost(e).asRight))(_ => {
                IO(println(s"Connection lost: ${e}"))
              })
              .unsafeRunSync()
          }

          def deliveryComplete(token: IMqttDeliveryToken): Unit = {
            ConcurrentEffect[IO]
              .runAsync(queue.enqueue1(DeliveryComplete().asRight))(_ => {
                IO(println(s"Delivery complete"))
              })
              .unsafeRunSync()
          }
        })
      })
      event <- queue.dequeue.rethrow
    } yield event
  }

}
