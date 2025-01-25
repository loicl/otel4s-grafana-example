import cats.MonadThrow
import cats.data.Kleisli
import cats.implicits.*
import org.http4s.{Header, Headers, HttpApp, Request, Response}
import org.typelevel.ci.CIString
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.context.propagation.{TextMapGetter, TextMapUpdater}
import org.typelevel.otel4s.trace.{SpanKind, Tracer}

object Http4sServerTracingMiddleware:
  // Implementation based upon Ivan Kurchenko's article:
  // https://ivan-kurchenko.medium.com/telemetry-with-scala-part-3-otel4s-c5c150303164

  extension [F[_]: {MonadThrow, Tracer}](service: HttpApp[F]) {

    /** Use this extension to activate distributed tracing on your Http4s endpoints evaluation.
      * @return
      *   returns a [[Kleisli]] representing the work by the Http4s on a [[Request]] that
      *   would return a [[Response]] but now the computation is traced
      */
    def traced: Kleisli[F, Request[F], Response[F]] = Kleisli { req =>
      Tracer[F].joinOrRoot(req.headers) {
        Tracer[F]
          .spanBuilder(s"${req.method.name} ${req.uri.path.toString}")
          .addAttributes(
            Attribute("http.request.method", req.method.name),
            Attribute("http.client_ip", req.remoteAddr.fold("<unknown>")(_.toString)),
            Attribute("http.request.body.size", req.contentLength.getOrElse(0L))
          )
          .withSpanKind(SpanKind.Server)
          .build
          .use { span =>
            for {
              response <- service(req)
              _ <- span.addAttributes(
                Attribute("http.status_code", response.status.code.toLong),
                Attribute("http.response.body.size", response.contentLength.getOrElse(0L))
              )
              headersWithTrace <- Tracer[F].propagate(response.headers)
            } yield {
              response.putHeaders(headersWithTrace)
            }
          }
      }
    }
  }

  /** A [[TextMapGetter]] is necessary for [[Tracer]] `joinOrRoot` to work. This one make
    * the propagate function work for [[Headers]] from `org.http4s`
    */
  given textMapGetter: TextMapGetter[Headers]:
    def get(headers: Headers, key: String): Option[String] =
      headers.get(CIString(key)).map(_.head.value)
    def keys(headers: Headers): Iterable[String] =
      headers.headers.map(_.name.toString)

  /** A [[TextMapUpdater]] is necessary for [[Tracer]] `propagate` to work. This one make
    * the `propagate` function work for [[Headers]] from `org.http4s`
    */
  given TextMapUpdater[Headers]:
    def updated(headers: Headers, key: String, value: String): Headers =
      headers.put(Header.Raw(CIString(key), value))