import cats.effect.*
import com.comcast.ip4s.{ipv4, port}
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.otel4s.metrics.Meter
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.trace.Tracer
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter

import Http4sServerTracingMiddleware.*

object Server extends IOApp {
  private def app[F[_]: {Async, LiftIO}]: Resource[F, Server] =
    for {
      httpClient: Client[F] <- EmberClientBuilder.default[F].build
      exampleService: FruitServiceClient[F] <- Resource.eval(
        FruitServiceClient[F](httpClient)
      )
      singleFruitPicker = new SingleFruitPicker(exampleService)
      tapir = FruitBagEndpoint.serverEndPoint[F](singleFruitPicker)
      serviceName = "otel4s-grafana-example-fruit-bag"
      server = ServerMaker.app(serviceName, tapir)
    } yield server

  override def run(args: List[String]): IO[ExitCode] =
    app[IO].useForever.as(ExitCode.Success)
}
