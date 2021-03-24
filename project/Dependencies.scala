import sbt._

object Dependencies {

  private object Versions {
    val zioVersion = "1.0.3"
    val zioNioVersion = "1.0.0-RC10"
    val circeVersion = "0.12.3"
    val zioInteropCatsVersion = "2.2.0.1"

    val disruptorVersion = "3.4.2"
    val zioLoggingVersion = "0.4.0"
    val http4sVersion = "0.21.4"
    val log4jVersion = "2.13.3"
    val h2Version = "1.4.200"
    val quillVersion = "3.5.1"
    val pureConfigVersion = "0.12.3"
    val scalaTestVersion = "3.2.3"
    val scoptVersion = "4.0.0"
  }

  import Versions._

  object Libraries {
    val http4s = Seq(
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
    )

    val zio = Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-macros" % zioVersion,
      "dev.zio" %% "zio-nio" % zioNioVersion,
      "dev.zio" %% "zio-nio-core" % zioNioVersion
    )

    val zioLogging = Seq(
      "dev.zio" %% "zio-logging" % zioLoggingVersion,
      "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion
    )

    val zioInteropCats = Seq("dev.zio" %% "zio-interop-cats" % zioInteropCatsVersion)

    val circe = Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion
    )

    val db = Seq(
      "com.h2database" % "h2" % h2Version,
      "io.getquill" %% "quill-jdbc" % quillVersion
    )
    val commons = Seq(
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "com.github.scopt" %% "scopt" % scoptVersion
    )
    val logging = Seq(
      "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion,
      "com.lmax" % "disruptor" % disruptorVersion
    )

    lazy val compileDependencies: Seq[ModuleID] = (
      http4s ++ zio ++ zioLogging ++ zioInteropCats ++
        circe ++ db ++ commons ++ logging) map (_ % Compile)

    lazy val testDependencies: Seq[ModuleID] = Seq(
      "dev.zio" %% "zio-test" % zioVersion,
      "dev.zio" %% "zio-test-sbt" % zioVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion
    ) map (_ % Test)
  }
}
