package arrow.test.laws

import arrow.typeclasses.Eq
import arrow.typeclasses.Hash
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object HashLaws {
  fun <F> laws(HF: Hash<F>, EQ: Eq<F>, cf: (Int) -> F): List<Law> =
    listOf(
      Law("Hash Laws: Equality implies equal hash") { equalHash(HF, EQ, cf) }
    )

  fun <F> equalHash(HF: Hash<F>, EQ: Eq<F>, cf: (Int) -> F): Unit {
    forAll(Gen.int()) { f ->
      val a = cf(f)
      val b = cf(f)
      EQ.run { a.eqv(b) } && HF.run { a.hash() == b.hash() }
    }
  }
}