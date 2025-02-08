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
  private def app[F[_]: {Async, LiftIO, Tracer, Meter}]: Resource[F, Server] =
    for {
      exampleService <- Resource.eval(FruitService[F](50, 500, 20))
      tapir = FruitsRoute.make[F](exampleService)
      serviceName = "otel4s-grafana-example-fruits"
      server = ServerMaker.app(serviceName, tapir)
    } yield server

  override def run(args: List[String]): IO[ExitCode] =
    given Meter[F] <- Resource.eval(otel.meterProvider.get(serviceName))
    given Tracer[F] <- Resource.eval(otel.tracerProvider.get(serviceName))
    app[IO].useForever.as(ExitCode.Success)
}
