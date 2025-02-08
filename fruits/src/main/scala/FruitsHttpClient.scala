import FruitEndpoint.exampleEndpoint
import cats.effect.Async
import org.http4s.{Request, Response, Uri}
import sttp.tapir.client.http4s.Http4sClientInterpreter

trait FruitsHttpClient[F[_]]:
  val fruitRequest: Request[F]
  val parseResponse: Response[F] => F[Either[Unit, Fruit]]

object FruitsHttpClient:
  def apply[F[_]: Async](): FruitsHttpClient[F] =
    new FruitsHttpClient[F]:
      val (
        fruitRequest: Request[F],
        parseResponse: (Response[F] => F[Either[Unit, Fruit]])
      ) =
        Http4sClientInterpreter[F]()
          .toRequestThrowDecodeFailures(
            exampleEndpoint,
            baseUri = Uri.fromString("htt://fruits:8080").toOption
          )
          .apply(())
