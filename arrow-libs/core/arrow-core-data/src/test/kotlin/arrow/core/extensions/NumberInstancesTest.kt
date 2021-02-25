package arrow.core.extensions

import arrow.core.byte
import arrow.core.double
import arrow.core.float
import arrow.core.int
import arrow.core.long
import arrow.core.short
import arrow.core.test.UnitSpec
import arrow.core.test.generators.byte
import arrow.core.test.generators.byteSmall
import arrow.core.test.generators.doubleSmall
import arrow.core.test.generators.floatSmall
import arrow.core.test.generators.intSmall
import arrow.core.test.generators.longSmall
import arrow.core.test.generators.short
import arrow.core.test.generators.shortSmall
import arrow.core.test.laws.MonoidLaws
import arrow.core.test.laws.SemiringLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.Monoid
import arrow.typeclasses.Semiring
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

class NumberInstancesTest : UnitSpec() {

  fun <F> testAllLaws(SG: Semiring<F>, M: Monoid<F>, GEN: Gen<F>, EQ: Eq<F>) {
    testLaws(SemiringLaws.laws(SG, GEN, EQ))
    testLaws(MonoidLaws.laws(M, GEN, EQ))
  }

  init {
    testAllLaws(Semiring.byte(), Monoid.byte(), Gen.byteSmall(), Eq.any())
    testAllLaws(Semiring.double(), Monoid.double(), Gen.doubleSmall(), Eq.any())
    testAllLaws(Semiring.int(), Monoid.int(), Gen.intSmall(), Eq.any())
    testAllLaws(Semiring.short(), Monoid.short(), Gen.shortSmall(), Eq.any())
    testAllLaws(Semiring.float(), Monoid.float(), Gen.floatSmall(), Eq.any())
    testAllLaws(Semiring.long(), Monoid.long(), Gen.longSmall(), Eq.any())

    /** Semigroup specific instance check */

    "should semigroup with the instance passed - int" {
      forAll { value: Int ->
        val seen = Monoid.int().run { value.combine(value) }
        val expected = value + value

        expected == seen
      }
    }

    "should semigroup with the instance passed - float" {
      forAll(Gen.numericFloats()) { value: Float ->
        val seen = Monoid.float().run { value.combine(value) }
        val expected = value + value

        expected == seen
      }
    }

    "should semigroup with the instance passed - double" {
      forAll(Gen.numericDoubles()) { value: Double ->
        val seen = Monoid.double().run { value.combine(value) }
        val expected = value + value

        expected == seen
      }
    }

    "should semigroup with the instance passed - long" {
      forAll { value: Long ->
        val seen = Monoid.long().run { value.combine(value) }
        val expected = value + value

        expected == seen
      }
    }

    "should semigroup with the instance passed - short" {
      forAll(Gen.short()) { value: Short ->
        val seen = Monoid.short().run { value.combine(value) }
        val expected = (value + value).toShort()

        expected == seen
      }
    }

    "should semigroup with the instance passed - byte" {
      forAll(Gen.byte()) { value: Byte ->
        val seen = Monoid.byte().run { value.combine(value) }
        val expected = (value + value).toByte()

        expected == seen
      }
    }
  }
}
