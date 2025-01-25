import org.typelevel.otel4s.metrics.{Counter, Meter}

object CounterFactory:
  def make[F[_]: Meter](countName: String, description: String): F[Counter[F, Long]] =
    Meter[F]
      .counter[Long](countName)
      .withDescription(description)
      .create