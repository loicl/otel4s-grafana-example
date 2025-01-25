import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object FruitEndpoint:
  val exampleEndpoint: Endpoint[Unit, Unit, Unit, Fruit, Any] =
    endpoint.get.in("fruit").out(jsonBody[Fruit])