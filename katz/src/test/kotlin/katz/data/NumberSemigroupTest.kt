package katz

import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class NumberSemigroupTest : UnitSpec() {
    init {
        "should semigroup with the instance passed" {
            forAll { value: Int ->
                val numberSemigroup = IntMonoid
                val seen = numberSemigroup.combine(value, value)
                val expected = value + value

                expected == seen
            }

            forAll { value: Float ->
                val numberSemigroup = FloatMonoid
                val seen = numberSemigroup.combine(value, value)
                val expected = value + value

                expected == seen
            }

            forAll { value: Double ->
                val numberSemigroup = DoubleMonoid
                val seen = numberSemigroup.combine(value, value)
                val expected = value + value

                expected == seen
            }

            forAll { value: Long ->
                val numberSemigroup = LongMonoid
                val seen = numberSemigroup.combine(value, value)
                val expected = value + value

                expected == seen
            }
        }
    }
}
