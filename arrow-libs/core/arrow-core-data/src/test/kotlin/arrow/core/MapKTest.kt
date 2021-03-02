package arrow.core

import arrow.core.test.UnitSpec
import arrow.core.test.generators.intSmall
import arrow.core.test.generators.longSmall
import arrow.core.test.generators.mapK
import arrow.core.test.laws.MonoidLaws
import arrow.typeclasses.Monoid
import arrow.typeclasses.Semigroup
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

class MapKTest : UnitSpec() {

  init {
    testLaws(MonoidLaws.laws(Monoid.map(Semigroup.int()), Gen.map(Gen.longSmall(), Gen.intSmall())))

    "can align maps" {
      // aligned keySet is union of a's and b's keys
      forAll(Gen.map(Gen.long(), Gen.bool()), Gen.map(Gen.long(), Gen.bool())) { a, b ->
        val aligned = a.align(b)
        aligned.size == (a.keys + b.keys).size
      }

      // aligned map contains Both for all entries existing in a and b
      forAll(Gen.map(Gen.long(), Gen.bool()), Gen.map(Gen.long(), Gen.bool())) { a, b ->
        val aligned = a.align(b)
        a.keys.intersect(b.keys).all {
          aligned[it]?.isBoth ?: false
        }
      }

      // aligned map contains Left for all entries existing only in a
      forAll(Gen.map(Gen.long(), Gen.bool()), Gen.map(Gen.long(), Gen.bool())) { a, b ->
        val aligned = a.align(b)
        (a.keys - b.keys).all { key ->
          aligned[key]?.let { it.isLeft } ?: false
        }
      }

      // aligned map contains Right for all entries existing only in b
      forAll(Gen.mapK(Gen.long(), Gen.bool()), Gen.mapK(Gen.long(), Gen.bool())) { a, b ->
        val aligned = a.align(b)
        (b.keys - a.keys).all { key ->
          aligned[key]?.let { it.isRight } ?: false
        }
      }
    }

    "zip" {
      forAll(
        Gen.map(Gen.intSmall(), Gen.intSmall()),
        Gen.map(Gen.intSmall(), Gen.intSmall())
      ) { a, b ->
        val result = a.zip(b) { _, left, right -> left + right }
        val expected: Map<Int, Int> = a.filter { (k, v) -> b.containsKey(k) }
          .map { (k, v) -> Pair(k, v + b[k]!!) }
          .toMap()
        result == expected
      }
    }

    "flatMap" {
      forAll(
        Gen.map(Gen.string(), Gen.intSmall()),
        Gen.map(Gen.string(), Gen.string())
      ) { a, b ->
        val result: Map<String, String> = a.flatMap { b }
        val expected: Map<String, String> = a.filter { (k, _) -> b.containsKey(k) }
          .map { (k, v) -> Pair(k, b[k]!!) }
          .toMap()
        result == expected
      }
    }
  }
}
