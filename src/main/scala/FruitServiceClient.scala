import Http4sServerTracingMiddleware.given
import cats.effect.Async
import cats.effect.kernel.Resource
import cats.implicits.*
import org.http4s.client.Client
import org.http4s.{Header, Headers, Response}
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.metrics.Meter
import org.typelevel.otel4s.trace.Tracer

trait FruitServiceClient[F[_]]:
  def downloadDataFromSomeAPI: F[Either[Unit, Fruit]]

object FruitServiceClient:
  def apply[F[_]: {Async, Tracer, Meter}](
      httpClient: Client[F]
  ): F[FruitServiceClient[F]] =
    CounterFactory
      .make("Download.fruit.count", "Number of fruits returned by the API.")
      .map { remoteApiFruitCount =>
        new FruitServiceClient[F] {
          private val traceSpan = Tracer[F].spanBuilder("fruits.bbc.co.uk/fruit").build
          private val fruitsHttpClient = FruitsHttpClient.apply[F]()
          private val fruitRequest = fruitsHttpClient.fruitRequest
          private val parseResponseF: Response[F] => F[Either[Unit, Fruit]] =
            fruitsHttpClient.parseResponse

          override def downloadDataFromSomeAPI: F[Either[Unit, Fruit]] =
            traceSpan.surround { downloadApiData() }

          def downloadApiData(): F[Either[Unit, Fruit]] =
            val tracedRequest: F[fruitRequest.Self] = for {
              traceIdHeader: Headers <- Tracer[F].propagate(Headers.empty)
              tracedReq = fruitRequest.putHeaders(traceIdHeader)
            } yield tracedReq
            val response: Resource[F, Response[F]] = for {
              req <- Resource.eval(tracedRequest)
              res <- httpClient.run(req)
            } yield res
            val apiData: F[Either[Unit, Fruit]] = response.use: res =>
              remoteApiFruitCount.inc(Attribute("example", "example"))
              Tracer[F].joinOrRoot(res.headers) { // not sure if needed
                parseResponseF(res)
              }
            apiData
        }
      }
