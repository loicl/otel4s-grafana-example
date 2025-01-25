import cats.effect.Async
import cats.implicits.*
import org.typelevel.otel4s.metrics.{Counter, Meter}

class SingleFruitPicker[F[_]: {Async, Meter}](fruitServiceClient: FruitServiceClient[F]):
  def pickSingleFruit()(using
      endpointCounterF: F[Counter[F, Long]]
  ): F[Either[Unit, Fruit]] =
    for {
      _ <- endpointCounterF.map(_.inc())
      fruit <- fruitServiceClient.downloadDataFromSomeAPI
    } yield fruit
