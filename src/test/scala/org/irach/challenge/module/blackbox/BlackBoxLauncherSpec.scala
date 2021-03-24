package org.irach.challenge.module.blackbox

import org.irach.challenge.environment.config.Configuration.BlackBoxConfig
import zio.test.DefaultRunnableSpec

object BlackBoxLauncherSpec extends DefaultRunnableSpec {

  import zio.test.Assertion._
  import zio.test._
  import zio.test.environment.TestEnvironment

  override def spec: ZSpec[TestEnvironment, Any] = suite("BlackBoxLauncher")(
    suite("To launch black box application")(
      testM("effect exception in either left") {
        // When
        val result = BlackBoxLauncher.launch(BlackBoxConfig("my application", "will fail"))
          .provideLayer(BlackBoxLauncher.live).either

        // Then
        assertM(result)(isLeft(isSubtype[Exception](anything)))
      },

      testM("effect ok in the right") {
        // When
        val result = BlackBoxLauncher.launch(BlackBoxConfig("echo 'hello'", "echo 'world'"))
          .provideLayer(BlackBoxLauncher.live).either

        // Then
        assertM(result)(isRight(containsString("world")))
      }
    )
  )
}
