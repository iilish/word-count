package org.irach.challenge.module

import org.irach.challenge.module.repo.Repository
import org.irach.challenge.module.state.{EventState, InternalState}
import zio.test.mock.mockable

object mock {

  @mockable[EventState.Service]
  object EventStateMock

  @mockable[InternalState.Service]
  object InternalStateMock

  @mockable[Repository.Service]
  object RepositoryMock

}
