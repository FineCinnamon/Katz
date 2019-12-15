package arrow.core

import arrow.Kind
import arrow.core.extensions.const.applicative.applicative
import arrow.core.extensions.const.eq.eq
import arrow.core.extensions.const.show.show
import arrow.core.extensions.const.traverseFilter.traverseFilter
import arrow.core.extensions.monoid
import arrow.test.UnitSpec
import arrow.test.generators.genConst
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
        TraverseFilterLaws.laws(Const.traverseFilter(),
          Const.applicative(this),
          Gen.genConst<Int, Int>(Gen.int()) as Gen<Kind<ConstPartialOf<Int>, Int>>,
          Eq.any()),
        ApplicativeLaws.laws(Const.applicative(this), Eq.any()),
        EqLaws.laws(Const.eq<Int, Int>(Eq.any()), Gen.genConst<Int, Int>(Gen.int())),
        ShowLaws.laws(Const.show(), Const.eq<Int, Int>(Eq.any()), Gen.genConst<Int, Int>(Gen.int()))
      )
    }
  }
}
