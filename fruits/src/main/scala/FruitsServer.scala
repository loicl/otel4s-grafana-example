import cats.effect.*
import cats.effect.std.Random
import com.comcast.ip4s.{ipv4, port}
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.otel4s.metrics.Meter
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.trace.Tracer
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter

import Http4sServerTracingMiddleware.*

object FruitsServer extends IOApp {
  private def app[F[_]: {Async, LiftIO}]: Resource[F, Server] =
    // noinspection ScalaUnusedSymbol
    given LoggerFactory[F] = Slf4jFactory.create[F]
    for {
      given Random[F] <- Resource.eval(Random.scalaUtilRandom[F])
      otel <- OtelJava.autoConfigured[F]()
      serviceName = "otel4s-grafana-example"
      given Meter[F] <- Resource.eval(otel.meterProvider.get(serviceName))
      given Tracer[F] <- Resource.eval(otel.tracerProvider.get(serviceName))
      exampleService <- Resource.eval(FruitService[F](50, 500, 20))
      tapir = FruitsRoute.make[F](exampleService)
      route = tapir2Route(List(tapir))
      server <- EmberServerBuilder
        .default[F]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(route.orNotFound.traced)
        .build
    } yield server

  private def tapir2Route[F[_]: Async](
      serverEndpoints: List[ServerEndpoint[Fs2Streams[F], F]]
  ): HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(serverEndpoints)

  override def run(args: List[String]): IO[ExitCode] =
    app[IO].useForever.as(ExitCode.Success)
}