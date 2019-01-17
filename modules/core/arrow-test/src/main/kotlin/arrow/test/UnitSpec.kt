package arrow.test

import arrow.test.laws.Law
import arrow.typeclasses.Eq
import io.kotlintest.TestCase
import io.kotlintest.TestType
import io.kotlintest.specs.AbstractStringSpec

/**
 * Base class for unit tests
 */
abstract class UnitSpec : AbstractStringSpec() {

  private val lawTestCases = mutableListOf<TestCase>()

  fun testLaws(vararg laws: List<Law>): List<TestCase> {
    return laws
     .flatMap { list: List<Law> -> list.asIterable() }
     .distinctBy { law: Law -> law.name }
     .map { law: Law ->
       val lawTestCase = createTestCase(law.name, law.test, defaultTestCaseConfig, TestType.Test)
       lawTestCases.add(lawTestCase)
       lawTestCase
     }
  }

  override fun testCases(): List<TestCase> {
    return lawTestCases
  }

}
