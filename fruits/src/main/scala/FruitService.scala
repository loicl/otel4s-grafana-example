import cats.effect.Async
import cats.effect.std.Random
import cats.implicits.*
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.metrics.Meter
import org.typelevel.otel4s.trace.Tracer

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

trait FruitService[F[_]] {
  def getDataFromSomeAPI: F[Fruit]
}

object FruitService {
  def apply[F[_]: {Async, Tracer, Meter, Random}](
      minLatency: Int,
      maxLatency: Int,
      bananaPercentage: Int
  ): F[FruitService[F]] = {
    CounterFactory
      .make("Fruits.fruit.count", "Number of fruits returned by the API.")
      .map { remoteApiFruitCount =>
        new FruitService[F] {
          private val spanBuilder = Tracer[F].spanBuilder("fruits.bbc.co.uk/fruit").build

          override def getDataFromSomeAPI: F[Fruit] = for {
            latency <- Random[F].betweenInt(minLatency, maxLatency)
            isBanana <- Random[F].betweenInt(0, 100).map(_ <= bananaPercentage)
            duration = FiniteDuration(latency, TimeUnit.MILLISECONDS)
            fruit <- spanBuilder.surround(
              Async[F].sleep(duration) *>
                Async[F].pure(if isBanana then "banana" else "apple")
            )
            _ <- remoteApiFruitCount.inc(Attribute("fruit", fruit))
          } yield Fruit(name = fruit)
        }
      }
  }
}
