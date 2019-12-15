package arrow.test.laws

import arrow.Kind
import arrow.core.extensions.eq
import arrow.core.extensions.list.foldable.fold
import arrow.test.generators.GenK
import arrow.typeclasses.Applicative
import arrow.typeclasses.Eq
import arrow.typeclasses.EqK
import arrow.typeclasses.MonoidK
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object MonoidKLaws {

  fun <F> laws(SGK: MonoidK<F>, GENK: GenK<F>, EQK: EqK<F>): List<Law> =
    laws(SGK, GENK.genK(Gen.int()), EQK.liftEq(Int.eq()))

  fun <F> laws(SGK: MonoidK<F>, AP: Applicative<F>, EQ: Eq<Kind<F, Int>>): List<Law> =
    laws(SGK, Gen.int().map { AP.just(it) }, EQ)

  fun <F> laws(SGK: MonoidK<F>, GEN: Gen<Kind<F, Int>>, EQ: Eq<Kind<F, Int>>): List<Law> =
    SemigroupKLaws.laws(SGK, GEN, EQ) + listOf(
      Law("MonoidK Laws: Left identity") { SGK.monoidKLeftIdentity(GEN, EQ) },
      Law("MonoidK Laws: Right identity") { SGK.monoidKRightIdentity(GEN, EQ) },
      Law("MonoidK Laws: Fold with Monoid instance") { SGK.monoidKFold(GEN, EQ) })


  fun <F> MonoidK<F>.monoidKLeftIdentity(GEN: Gen<Kind<F, Int>>, EQ: Eq<Kind<F, Int>>): Unit =
    forAll(GEN) { fa: Kind<F, Int> ->
      empty<Int>().combineK(fa).equalUnderTheLaw(fa, EQ)
    }

  fun <F> MonoidK<F>.monoidKRightIdentity(GEN: Gen<Kind<F, Int>>, EQ: Eq<Kind<F, Int>>): Unit =
    forAll(GEN) { fa: Kind<F, Int> ->
      fa.combineK(empty<Int>()).equalUnderTheLaw(fa, EQ)
    }

  fun <F> MonoidK<F>.monoidKFold(GEN: Gen<Kind<F, Int>>, EQ: Eq<Kind<F, Int>>) {
    val mo = this
    forAll(GEN) { fa: Kind<F, Int> ->
      listOf(fa).fold(mo.algebra()).equalUnderTheLaw(fa, EQ)
    }
  }
}
