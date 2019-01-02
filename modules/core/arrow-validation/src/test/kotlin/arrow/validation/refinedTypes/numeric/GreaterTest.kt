package arrow.validation.refinedTypes.numeric

import arrow.instances.order
import arrow.test.UnitSpec
import arrow.test.generators.genGreater
import arrow.test.generators.genLessEqual
import arrow.validation.refinedTypes.numeric.validated.greater.greater
import io.kotlintest.properties.forAll
import io.kotlintest.runner.junit4.KotlinTestRunner
import org.junit.runner.RunWith

@RunWith(KotlinTestRunner::class)
class GreaterTest : UnitSpec() {
  init {
    val min = 100

    "Can create Greater for every number greater than the min defined by instace" {
      forAll(genGreater(min)) { x: Int ->
        x.greater(Int.order(), min).isValid
      }
    }

    "Can not create Greater for any number less or equal than the min defined by instance" {
      forAll(genLessEqual(min)) { x: Int ->
        x.greater(Int.order(), min).isInvalid
      }
    }
  }

}