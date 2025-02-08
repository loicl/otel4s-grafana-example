import sttp.tapir.AnyEndpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

//noinspection ScalaUnusedSymbol
object SwaggerMaker:
  private val interpreter = SwaggerInterpreter()
  def fromEndpoints[F[_]](
      endpoints: List[AnyEndpoint],
      title: String,
      version: String
  ): Seq[ServerEndpoint[Any, F]] =
    interpreter.fromEndpoints(endpoints, title, version)
