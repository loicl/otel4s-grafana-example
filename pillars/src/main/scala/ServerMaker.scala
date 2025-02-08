import sttp.tapir.AnyEndpoint
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

object ServerMaker:
  def app[F[_]: {Async, LiftIO}](
      serviceName: String,
      endpoints: List[ServerEndpoint[Any, F]]
  ): Resource[F, Server] =
    // noinspection ScalaUnusedSymbol
    given LoggerFactory[F] = Slf4jFactory.create[F]
    for {
      otel <- OtelJava.autoConfigured[F]()
      given Meter[F] <- Resource.eval(otel.meterProvider.get(serviceName))
      given Tracer[F] <- Resource.eval(otel.tracerProvider.get(serviceName))
      route: HttpRoutes[F] = tapir2Route(endpoints)
      server <- EmberServerBuilder
        .default[F]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(route.orNotFound.traced)
        .build
    } yield server

  private def tapir2Route[F[_]: Async](
      serverEndpoints: List[ServerEndpoint[Any, F]]
  ): HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(serverEndpoints)
