package arrow.core.computations

import arrow.core.test.UnitSpec
import io.kotlintest.shouldBe

class IterableEffectTest : UnitSpec() {

  init {
    "empty - expect 2" {
      val result: String? = emptyList<Int>().iterateOrNull {
        val a = next()
        val b = next()
        "$a $b"
      }
      result shouldBe null
    }
    "single - expect 1" {
      val result: String? = listOf(1).iterateOrNull {
        val a = next()
        "$a"
      }
      result shouldBe "1"
    }
    "single - expect 2" {
      val result: String? = listOf(1).iterateOrNull {
        val a = next()
        val b = next()
        "$a $b"
      }
      result shouldBe null
    }
    "double - expect 2" {
      val result: String? = listOf(1, 2).iterateOrNull {
        val a = next()
        val b = next()
        "$a $b"
      }
      result shouldBe "1 2"
    }
    "drop middle values" {
      val result: String? = listOf(1, 2, 3, 4).iterateOrNull {
        val a = next()
        drop(2)
        val b = next()
        "$a $b"
      }
      result shouldBe "1 4"
    }
    "drop second value" {
      val result: String? = listOf(1, 2, 3).iterateOrNull {
        val a = next()
        dropNext()
        val b = next()
        "$a $b"
      }
      result shouldBe "1 3"
    }
    "drop from empty" {
      val result: String? = emptyList<Int>().iterateOrNull {
        drop(1)
        "won't see me"
      }
      result shouldBe null
    }
    "cancel iteration" {
      val result: String? = listOf<Number>(1, 2.0).iterateOrNull {
        val a = next()
        val b = next() as? Long ?: cancel()
        "$a $b"
      }
      result shouldBe null
    }
  }
}
