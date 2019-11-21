package arrow.core

import arrow.Kind
import arrow.Kind2
import arrow.core.extensions.eq
import arrow.core.extensions.hash
import arrow.core.extensions.mapk.align.align
import arrow.core.extensions.mapk.eq.eq
import arrow.core.extensions.mapk.foldable.foldable
import arrow.core.extensions.mapk.functor.functor
import arrow.core.extensions.mapk.functorFilter.functorFilter
import arrow.core.extensions.mapk.hash.hash
import arrow.core.extensions.mapk.monoid.monoid
import arrow.core.extensions.mapk.semialign.semialign
import arrow.core.extensions.mapk.show.show
import arrow.core.extensions.mapk.traverse.traverse
import arrow.core.extensions.semigroup
import arrow.test.UnitSpec
import arrow.test.generators.mapK
import arrow.test.laws.AlignLaws
import arrow.test.laws.EqLaws
import arrow.test.laws.FoldableLaws
import arrow.test.laws.FunctorFilterLaws
import arrow.test.laws.HashLaws
import arrow.test.laws.MonoidLaws
import arrow.test.laws.ShowLaws
import arrow.test.laws.TraverseLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.EqK
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

class MapKTest : UnitSpec() {

  val EQ: Eq<Kind2<ForMapK, String, Int>> = object : Eq<Kind2<ForMapK, String, Int>> {
    override fun Kind2<ForMapK, String, Int>.eqv(b: Kind2<ForMapK, String, Int>): Boolean =
      fix()["key"] == b.fix()["key"]
  }

  val EQK = object : EqK<MapKPartialOf<String>> {
    override fun <A> Kind<MapKPartialOf<String>, A>.eqK(other: Kind<MapKPartialOf<String>, A>, EQ: Eq<A>): Boolean =
      MapK.eq(String.eq(), EQ).run { this@eqK.fix().eqv(other.fix()) }
  }

  init {
    val EQ_TC = MapK.eq(String.eq(), Int.eq())

    val testLaws = testLaws(
      ShowLaws.laws(MapK.show(), EQ_TC) { mapOf(it.toString() to it).k() },
      TraverseLaws.laws(MapK.traverse(), MapK.functor(), { a: Int -> mapOf("key" to a).k() }, EQ),
      MonoidLaws.laws(MapK.monoid<String, Int>(Int.semigroup()), Gen.mapK(Gen.string(), Gen.int()), EQ),
      FoldableLaws.laws(MapK.foldable(), { a: Int -> mapOf("key" to a).k() }, Eq.any()),
      EqLaws.laws(EQ) { mapOf(it.toString() to it).k() },
      FunctorFilterLaws.laws(MapK.functorFilter(), { mapOf(it.toString() to it).k() }, EQ),
      HashLaws.laws(MapK.hash(String.hash(), Int.hash()), EQ_TC) { mapOf("key" to it).k() },
      AlignLaws.foldablelaws(MapK.align<String>(),
        Gen.mapK(Gen.string(), Gen.int()) as Gen<Kind<MapKPartialOf<String>, Int>>,
        EQK,
        MapK.foldable()
      )
    )

    "can align maps" {
      // aligned keySet is union of a's and b's keys
      forAll(Gen.mapK(Gen.string(), Gen.bool()), Gen.mapK(Gen.string(), Gen.bool())) { a, b ->
        MapK.semialign<String>().run {
          val aligned = align(a, b).fix()

          aligned.size == (a.keys + b.keys).size
        }
      }

      // aligned map contains Both for all entries existing in a and b
      forAll(Gen.mapK(Gen.string(), Gen.bool()), Gen.mapK(Gen.string(), Gen.bool())) { a, b ->
        MapK.semialign<String>().run {
          val aligned = align(a, b).fix()
          a.keys.intersect(b.keys).all {
            aligned[it]?.isBoth ?: false
          }
        }
      }

      // aligned map contains Left for all entries existing only in a
      forAll(Gen.mapK(Gen.string(), Gen.bool()), Gen.mapK(Gen.string(), Gen.bool())) { a, b ->
        MapK.semialign<String>().run {
          val aligned = align(a, b).fix()
          (a.keys - b.keys).all { key ->
            aligned[key]?.let { it.isLeft } ?: false
          }
        }
      }

      // aligned map contains Right for all entries existing only in b
      forAll(Gen.mapK(Gen.string(), Gen.bool()), Gen.mapK(Gen.string(), Gen.bool())) { a, b ->
        MapK.semialign<String>().run {
          val aligned = align(a, b).fix()
          (b.keys - a.keys).all { key ->
            aligned[key]?.let { it.isRight } ?: false
          }
        }
      }
    }
  }
}
