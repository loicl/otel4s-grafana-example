import cats.effect.Async
import cats.implicits.*
import org.typelevel.otel4s.metrics.{Counter, Meter}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint.Full

object FruitBagEndpoint:
  opaque type FruitCount = Int
  opaque type FruitBagEndpointT = Endpoint[Unit, FruitCount, Unit, FruitBag, Any]
  // opaque type FruitBagServerEndpointT[F[_]] =
  //   Full[Unit, Unit, FruitCount, Unit, FruitBag, Any, F]

  private val fruitCountPath: EndpointInput.PathCapture[FruitCount] =
    path[FruitCount]("fruit-count")
      .validate(Validator.positive)
      .description("The number of fruits you want to pick and bag.")
  val fruitBagEndpoint: FruitBagEndpointT =
    endpoint.get.in("bag").in(fruitCountPath).out(jsonBody[FruitBag])

  def serverEndPoint[F[_]: {Async, Meter}](
      singleFruitPicker: SingleFruitPicker[F]
  ): Full[Unit, Unit, FruitCount, Unit, FruitBag, Any, F] =

    given endpointCounterF: F[Counter[F, Long]] =
      CounterFactory.make(
        "Download.count",
        "number of times the route /example is called"
      )

    fruitBagEndpoint.serverLogicSuccess[F] { (count: Int) =>
      val fruitPicks: Seq[F[Either[Unit, Fruit]]] = (1 to count).map { _ =>
        singleFruitPicker.pickSingleFruit()
      } // ).sequence
      val goodFruits: F[Seq[Either[Unit, Fruit]]] = fruitPicks.sequence
      goodFruits.map { s => s.collect { case Right(fruit) => fruit } } map (FruitBag(_))
    }
