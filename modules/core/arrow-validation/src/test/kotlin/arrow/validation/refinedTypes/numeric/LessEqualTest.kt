package arrow.validation.refinedTypes.numeric

import arrow.instances.order
import arrow.test.UnitSpec
import arrow.test.generators.genGreater
import arrow.test.generators.genLessEqual
import arrow.validation.refinedTypes.numeric.validated.lessEqual.lessEqual
import io.kotlintest.properties.forAll
import io.kotlintest.runner.junit4.KotlinTestRunner
import org.junit.runner.RunWith

@RunWith(KotlinTestRunner::class)
class LessEqualTest : UnitSpec() {
  init {

    val max = 100

    "Can create LessEqual for every number less or equal than min defined by instance" {
      forAll(genLessEqual(max)) { x: Int ->
        x.lessEqual(Int.order(), max).isValid
      }
    }

    "Can not create LessEqual for any number greater than min defined by instance" {
      forAll(genGreater(max)) { x: Int ->
        x.lessEqual(Int.order(), max).isInvalid
      }
    }

  }

}