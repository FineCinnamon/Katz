package arrow.optics.instances

import arrow.core.test.UnitSpec
import arrow.core.test.generators.functionAToB
import arrow.core.test.generators.option
import arrow.optics.Traversal
import arrow.optics.option
import arrow.optics.test.laws.TraversalLaws
import io.kotlintest.properties.Gen

class OptionInstanceTest : UnitSpec() {

  init {

    testLaws(
      TraversalLaws.laws(
        traversal = Traversal.option(),
        aGen = Gen.option(Gen.string()),
        bGen = Gen.string(),
        funcGen = Gen.functionAToB(Gen.string()),
      )
    )
  }
}
