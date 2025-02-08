ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.3"

lazy val root = (project in file("."))
  .settings(
    name := "rms-10pc-sttp-distributed-tracing",
    dockerExposedPorts ++= Seq(8080),
    dockerBaseImage := "eclipse-temurin:21",
    mainClass := Some("Server"),
    libraryDependencies ++= commonDeps ++ circeDeps ++ http4sDeps ++ logDeps ++ otelDeps,
    scalacOptions ++= Seq(
      "-Wunused:all",
      "-Wvalue-discard",
      "-language:implicitConversions",
      "-source:future",
      "-feature",
      "-deprecation"
    ),
    Compile / javaOptions ++= Seq(
      "-Dotel.java.global-autoconfigure.enabled=true",
      s"-Dotel.service.name=${name.value}"
    ),
    Universal / javaOptions ++= (Compile / javaOptions).value,
    fork := true,
    scalafmtOnCompile := true
  )
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(ScalafmtPlugin)
  .dependsOn(pillars, fruits)

lazy val fruits = (project in file("./fruits"))
  .settings(
    name := "fruits",
    dockerExposedPorts ++= Seq(8080),
    dockerBaseImage := "eclipse-temurin:21",
    mainClass := Some("FruitsServer"),
    libraryDependencies ++= commonDeps ++ circeDeps ++ http4sDeps ++ logDeps ++ otelDeps,
    scalacOptions ++= Seq(
      "-Wunused:all",
      "-Wvalue-discard",
      "-language:implicitConversions",
      "-source:future",
      "-feature",
      "-deprecation"
    ),
    Compile / javaOptions ++= Seq(
      "-Dotel.java.global-autoconfigure.enabled=true",
      s"-Dotel.service.name=${name.value}"
    ),
    Universal / javaOptions ++= (Compile / javaOptions).value,
    fork := true,
    scalafmtOnCompile := true
  )
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(ScalafmtPlugin)
  .dependsOn(pillars)

lazy val pillars = (project in file("./pillars"))
  .settings(
    name := "pillars",
    libraryDependencies ++= commonDeps ++ circeDeps ++ http4sDeps ++ logDeps ++ otelDeps,
    scalacOptions ++= Seq(
      "-Wunused:all",
      "-Wvalue-discard",
      "-language:implicitConversions",
      "-source:future",
      "-feature",
      "-deprecation"
    ),
    Compile / javaOptions ++= Seq(
      "-Dotel.java.global-autoconfigure.enabled=true",
      s"-Dotel.service.name=${name.value}"
    ),
    Universal / javaOptions ++= (Compile / javaOptions).value,
    fork := true,
    scalafmtOnCompile := true
  ).enablePlugins(ScalafmtPlugin)

lazy val commonDeps = Seq(deps.cats, deps.catsEffect)
lazy val circeDeps = Seq(deps.circe, deps.circeGeneric)
lazy val http4sDeps = Seq(deps.http4sServer, deps.http4sDsl, deps.http4sCirce,
  deps.tapirHttp4s, deps.tapirCirce, deps.tapirClient, deps.tapirSwagger, deps.http4sEmberClient)
lazy val logDeps = Seq(deps.log4cats, deps.logback)
lazy val otelDeps = Seq(deps.otel4s, deps.otelExporter, deps.otelSdk)

lazy val deps = new {
  val catsVersion = "2.13.0"
  val catsEffectVersion = "3.5.7"
  val circeVersion = "0.14.10"
  val http4sVersion = "0.23.30"
  val otel4sVersion = "0.11.2"
  val otelVersion = "1.46.0"
  val slf4jVersion = "2.0.5"
  val log4catsVersion = "2.7.0"
  val logbackVersion = "1.5.16"
  val tapirVersion = "1.11.13"

  val cats = "org.typelevel" %% "cats-core" % catsVersion
  val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion

  val circe = "io.circe" %% "circe-core" % circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion

  val http4sServer = "org.http4s" %% "http4s-ember-server" % http4sVersion
  val http4sDsl = "org.http4s" %% "http4s-dsl" % http4sVersion
  val http4sCirce = "org.http4s" %% "http4s-circe" % http4sVersion
  val http4sEmberClient = "org.http4s" %% "http4s-ember-client" % http4sVersion

  val log4cats = "org.typelevel" %% "log4cats-slf4j" % log4catsVersion
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion

  val otel4s = "org.typelevel" %% "otel4s-oteljava" % otel4sVersion
  val otelExporter =
    "io.opentelemetry" % "opentelemetry-exporter-otlp" % otelVersion % Runtime
  val otelSdk =
    "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % otelVersion % Runtime

  val tapirHttp4s = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion
  val tapirCirce = "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion

  val tapirClient = "com.softwaremill.sttp.tapir" %% "tapir-http4s-client" % tapirVersion
  val tapirSwagger = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion
}