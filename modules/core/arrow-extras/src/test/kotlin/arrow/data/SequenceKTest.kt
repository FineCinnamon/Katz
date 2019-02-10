package arrow.data

import arrow.Kind
import arrow.core.Tuple2
import arrow.core.extensions.eq
import arrow.core.extensions.hash
import arrow.data.extensions.sequencek.applicative.applicative
import arrow.data.extensions.sequencek.eq.eq
import arrow.data.extensions.sequencek.hash.hash
import arrow.data.extensions.sequencek.monad.monad
import arrow.data.extensions.sequencek.monoid.monoid
import arrow.data.extensions.sequencek.monoidK.monoidK
import arrow.data.extensions.sequencek.semigroupal.semigroupal
import arrow.data.extensions.sequencek.traverse.traverse
import arrow.test.UnitSpec
import arrow.test.generators.sequenceK
import arrow.test.laws.*
import arrow.typeclasses.Eq
import arrow.typeclasses.Show
import io.kotlintest.properties.Gen
import io.kotlintest.runner.junit4.KotlinTestRunner
import org.junit.runner.RunWith

@RunWith(KotlinTestRunner::class)
class SequenceKTest : UnitSpec() {

  val A: SequenceK<Int> = sequenceOf(1,2).k()
  val B: SequenceK<Int> = sequenceOf(3,4).k()
  val C: SequenceK<Int> = sequenceOf(5).k()

  init {

    val eq: Eq<Kind<ForSequenceK, Int>> = object : Eq<Kind<ForSequenceK, Int>> {
      override fun Kind<ForSequenceK, Int>.eqv(b: Kind<ForSequenceK, Int>): Boolean =
        toList() == b.toList()
    }

    val show: Show<Kind<ForSequenceK, Int>> = object : Show<Kind<ForSequenceK, Int>> {
      override fun Kind<ForSequenceK, Int>.show(): String =
        toList().toString()
    }

    testLaws(
      ShowLaws.laws(show, eq) { sequenceOf(it).k() },
      MonadLaws.laws(SequenceK.monad(), eq),
      MonoidKLaws.laws(SequenceK.monoidK(), SequenceK.applicative(), eq),
      MonoidLaws.laws(SequenceK.monoid(), Gen.sequenceK(Gen.int()), eq),
      SemigroupalLaws.laws(SequenceK.semigroupal(), A, B, C, this::bijection),
      TraverseLaws.laws(SequenceK.traverse(), SequenceK.applicative(), { n: Int -> SequenceK(sequenceOf(n)) }, eq),
      HashLaws.laws(SequenceK.hash(Int.hash()), SequenceK.eq(Int.eq())) { sequenceOf(it).k() }
    )
  }

  private fun bijection(from: Kind<ForSequenceK, Tuple2<Tuple2<Int, Int>, Int>>): SequenceK<Tuple2<Int, Tuple2<Int, Int>>> =
          from.fix().toList().map { Tuple2(it.a.a, Tuple2(it.a.b, it.b)) }.asSequence().k()
}
