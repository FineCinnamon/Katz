package arrow.data

import arrow.core.Function1
import arrow.core.Function1Of
import arrow.core.invoke
import arrow.core.monad
import arrow.instances.Function1
import arrow.test.UnitSpec
import arrow.test.laws.MonadLaws
import arrow.typeclasses.Eq
import io.kotlintest.KTestJUnitRunner
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class Function1Test : UnitSpec() {
  val EQ: Eq<Function1Of<Int, Int>> = Eq { a, b ->
    a(1) == b(1)
  }

  init {
    Function1<Int>() syntax {
      testLaws(MonadLaws.laws(this, EQ))
    }
  }
}
