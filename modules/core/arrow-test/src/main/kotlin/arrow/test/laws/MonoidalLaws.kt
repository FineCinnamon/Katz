package arrow.test.laws

import arrow.Kind
import arrow.core.Tuple2
import arrow.core.extensions.eq
import arrow.core.extensions.tuple2.eq.eq
import arrow.test.generators.GenK
import arrow.typeclasses.Eq
import arrow.typeclasses.EqK
import arrow.typeclasses.Monoidal
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object MonoidalLaws {

  fun <F> laws(
    MDAL: Monoidal<F>,
    GENK: GenK<F>,
    EQ: EqK<F>,
    BIJECTION: (Kind<F, Tuple2<Tuple2<Int, Int>, Int>>) -> (Kind<F, Tuple2<Int, Tuple2<Int, Int>>>),
    ASSOCIATIVE_SEMIGROUPAL_EQ: Eq<Kind<F, Tuple2<Int, Tuple2<Int, Int>>>>
  ): List<Law> =
    laws(
      MDAL,
      GENK.genK(Gen.int()),
      EQ.liftEq(Tuple2.eq(Int.eq(), Int.eq())),
      BIJECTION,
      ASSOCIATIVE_SEMIGROUPAL_EQ
    )

  fun <F> laws(
    MDAL: Monoidal<F>,
    GEN: Gen<Kind<F, Int>>,
    EQ: Eq<Kind<F, Tuple2<Int, Int>>>,
    BIJECTION: (Kind<F, Tuple2<Tuple2<Int, Int>, Int>>) -> (Kind<F, Tuple2<Int, Tuple2<Int, Int>>>),
    ASSOCIATIVE_SEMIGROUPAL_EQ: Eq<Kind<F, Tuple2<Int, Tuple2<Int, Int>>>>
  ): List<Law> =
    SemigroupalLaws.laws(MDAL, GEN, BIJECTION, ASSOCIATIVE_SEMIGROUPAL_EQ) + listOf(
      Law("Monoidal Laws: Left identity") { MDAL.monoidalLeftIdentity(GEN, EQ) },
      Law("Monoidal Laws: Right identity") { MDAL.monoidalRightIdentity(GEN, EQ) }
    )

  private fun <F> Monoidal<F>.monoidalLeftIdentity(G: Gen<Kind<F, Int>>, EQ: Eq<Kind<F, Tuple2<Int, Int>>>): Unit =
    forAll(G) { fa: Kind<F, Int> ->
      identity<Int>().product(fa).equalUnderTheLaw(identity(), EQ)
    }

  private fun <F> Monoidal<F>.monoidalRightIdentity(G: Gen<Kind<F, Int>>, EQ: Eq<Kind<F, Tuple2<Int, Int>>>): Unit =
    forAll(G) { fa: Kind<F, Int> ->
      fa.product(identity<Int>()).equalUnderTheLaw(identity(), EQ)
    }
}
