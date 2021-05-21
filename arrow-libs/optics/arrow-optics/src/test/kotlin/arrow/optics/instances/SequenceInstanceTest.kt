package arrow.optics.instances

import arrow.core.test.UnitSpec
import arrow.core.test.generators.functionAToB
import arrow.core.test.generators.sequence
import arrow.optics.Traversal
import arrow.optics.test.laws.OptionalLaws
import arrow.optics.test.laws.TraversalLaws
import arrow.optics.typeclasses.FilterIndex
import arrow.optics.typeclasses.Index
import io.kotest.property.Arb

class SequenceInstanceTest : UnitSpec() {

  init {

    testLaws(
      TraversalLaws.laws(
        traversal = Traversal.sequence(),
        aGen = Gen.sequence(Arb.string()),
        bGen = Arb.string(),
        funcGen = Arb.functionAToB(Arb.string()),
        eq = { a, b -> a.toList() == b.toList() }
      )
    )

    testLaws(
      TraversalLaws.laws(
        traversal = FilterIndex.sequence<String>().filter { true },
        aGen = Gen.sequence(Arb.string()),
        bGen = Arb.string(),
        funcGen = Arb.functionAToB(Arb.string()),
        eq = { a, b -> a.toList() == b.toList() }
      )
    )

    testLaws(
      OptionalLaws.laws(
        optionalGen = Arb.int().map { Index.sequence<String>().index(it) },
        aGen = Gen.sequence(Arb.string()),
        bGen = Arb.string(),
        funcGen = Arb.functionAToB(Arb.string()),
        eqa = { a, b -> a.toList() == b.toList() }
      )
    )
  }
}
