import io.circe.derivation.{Configuration, ConfiguredCodec}

given Configuration = Configuration.default
case class Fruit(name: String) derives ConfiguredCodec
case class FruitBag(fruits: Seq[Fruit]) derives ConfiguredCodec