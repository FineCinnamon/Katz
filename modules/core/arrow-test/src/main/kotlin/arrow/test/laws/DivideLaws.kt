package arrow.test.laws

import arrow.Kind
import arrow.core.Tuple2
import arrow.core.toT
import arrow.typeclasses.Divide
import arrow.typeclasses.Eq
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object DivideLaws {

  fun <F> laws(
    DF: Divide<F>,
    G: Gen<Kind<F, Int>>,
    EQ: Eq<Kind<F, Int>>
  ): List<Law> = ContravariantLaws.laws(DF, G, EQ) + listOf(
    Law("Divide laws: Associative") { DF.associative(G, EQ) }
  )

  fun <A> delta(a: A): Tuple2<A, A> = a toT a

  fun <F> Divide<F>.associative(
    G: Gen<Kind<F, Int>>,
    EQ: Eq<Kind<F, Int>>
  ): Unit =
    forAll(G) { fa ->
      val a = divide<Int, Int, Int>(
        fa,
        divide(fa, fa) { delta(it) }
      ) { delta(it) }

      val b = divide<Int, Int, Int>(
        divide(fa, fa) { delta(it) },
        fa
      ) { delta(it) }

      a.equalUnderTheLaw(b, EQ)
    }
}
