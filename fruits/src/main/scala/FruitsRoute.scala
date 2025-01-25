import FruitEndpoint.exampleEndpoint
import cats.effect.Async
import cats.implicits.*
import org.typelevel.otel4s.metrics.{Counter, Meter}
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint.Full

object FruitsRoute {
  def make[F[_]: {Async, Meter}](
      exampleService: FruitService[F]
  ): Full[Unit, Unit, Unit, Unit, Fruit, Any, F] =
    val endpointCounterF: F[Counter[F, Long]] =
      CounterFactory.make("Fruit.count", "number of times the route /fruit is called")

    exampleEndpoint.serverLogicSuccess[F] { _ =>
      for {
        endpointCounter <- endpointCounterF
        _ <- endpointCounter.inc()
        data <- exampleService.getDataFromSomeAPI
      } yield data
    }
}