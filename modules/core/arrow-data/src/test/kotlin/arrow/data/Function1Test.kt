package arrow.data

import arrow.core.*
import arrow.instances.ForFunction1
import arrow.instances.monoid
import arrow.instances.semigroup
import arrow.test.UnitSpec
import arrow.test.laws.MonadLaws
import arrow.test.laws.MonoidLaws
import arrow.test.laws.SemigroupLaws
import arrow.typeclasses.Eq
import io.kotlintest.KTestJUnitRunner
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class Function1Test : UnitSpec() {
  val EQ: Eq<Function1Of<Int, Int>> = Eq { a, b ->
    a(1) == b(1)
  }

  init {
    ForFunction1<Int>() extensions {
      testLaws(
        SemigroupLaws.laws(Function1.semigroup<Int, Int>(Int.semigroup()), Function1 { 1 }, Function1 { 2 }, Function1 { 3 }, EQ),
        MonoidLaws.laws(Function1.monoid<Int, Int>(Int.monoid()), Function1 { it }, EQ),
        MonadLaws.laws(this, EQ)
      )
    }
  }
}
