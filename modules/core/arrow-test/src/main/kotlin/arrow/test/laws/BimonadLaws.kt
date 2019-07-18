package arrow.test.laws

import arrow.Kind
import arrow.typeclasses.Bimonad
import arrow.typeclasses.Eq
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object BimonadLaws {
  fun <F> laws(BF: Bimonad<F>,
               f: (Int) -> Kind<F, Kind<F, Int>>,
               EQ1: Eq<Int>,
               EQ2: Eq<Kind<F, Kind<F, Int>>>): List<Law> =
    listOf(
      Law("Bimonad Laws: Extract Identity") { BF.extractIsIdentity(EQ1) },
      Law("Bimonad Laws: CoflatMap Composition") { BF.coflatMapComposition(EQ2) },
      Law("Bimonad Laws: Extract FlatMap") { BF.extractFlatMap(f, EQ1) }
    )

  fun <F> Bimonad<F>.extractIsIdentity(EQ: Eq<Int>): Unit =
    forAll(
      Gen.int()
    ) { a ->
      just(a).extract().equalUnderTheLaw(a, EQ)
    }

  fun <F> Bimonad<F>.extractFlatMap(f: (Int) -> Kind<F, Kind<F, Int>>, EQ: Eq<Int>): Unit =
    forAll(
      Gen.int().map(f)
    ) { ffa ->
      ffa.flatten().extract().equalUnderTheLaw(ffa.map { it.extract() }.extract(), EQ)
    }

  fun <F> Bimonad<F>.coflatMapComposition(EQ: Eq<Kind<F, Kind<F, Int>>>): Unit =
    forAll(
      Gen.int()
    ) { a ->
      just(a).coflatMap { it }.equalUnderTheLaw(just(a).map { just(it) }, EQ) ==
        just(a).coflatMap { it }.equalUnderTheLaw(just(a).duplicate(), EQ)
    }

}
