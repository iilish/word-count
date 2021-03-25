import Dependencies.Libraries.{compileDependencies, testDependencies}

name := "zio-tech-challenge"
version := "1.0.1-SNAPSHOT"
scalaVersion := "2.13.4"
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

lazy val settings = Seq(
  scalacOptions += "-Ymacro-annotations",
  libraryDependencies ++= compileDependencies ++ testDependencies,
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
)

lazy val root = (project in file("."))
  .settings(settings)

assembly / mainClass := Some("org.irach.challenge.WordCountApp")

assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = true)
