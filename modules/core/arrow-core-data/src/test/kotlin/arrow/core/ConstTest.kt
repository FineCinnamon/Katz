package arrow.core

import arrow.core.extensions.const.applicative.applicative
import arrow.core.extensions.const.eq.eq
import arrow.core.extensions.const.show.show
import arrow.core.extensions.const.traverseFilter.traverseFilter
import arrow.core.extensions.monoid
import arrow.test.UnitSpec
import arrow.test.laws.ApplicativeLaws
import arrow.test.laws.EqLaws
import arrow.test.laws.ShowLaws
import arrow.test.laws.TraverseFilterLaws
import arrow.typeclasses.Eq
import io.kotlintest.properties.Gen

class ConstTest : UnitSpec() {
  init {
    Int.monoid().run {
      testLaws(
        TraverseFilterLaws.laws(Const.traverseFilter(), Const.applicative(this), { Const(it) }, Eq.any()),
        ApplicativeLaws.laws(Const.applicative(this), Eq.any()),
        EqLaws.laws(Const.eq<Int, Int>(Eq.any()), Gen.const(Gen.int())),
        ShowLaws.laws(Const.show(), Const.eq<Int, Int>(Eq.any()), Gen.const(Gen.int()))
      )
    }
  }
}

private fun <A> Gen.Companion.const(gen: Gen<A>): Gen<Const<A, A>> =
  gen.map {
    Const<A, A>(it)
  }
