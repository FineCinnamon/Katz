package arrow.optics

import arrow.core.test.UnitSpec
import arrow.core.test.generators.either
import arrow.core.test.generators.functionAToB
import arrow.optics.test.laws.OptionalLaws
import arrow.optics.test.laws.PrismLaws
import arrow.optics.test.laws.SetterLaws
import arrow.optics.test.laws.TraversalLaws
import arrow.typeclasses.Monoid
import io.kotest.property.Arb
import io.kotest.property.checkAll

class PrismTest : UnitSpec() {

  init {
    testLaws(
      PrismLaws.laws(
        prism = sumPrism,
        aGen = genSum,
        bGen = Arb.string(),
        funcGen = Arb.functionAToB(Arb.string()),
      ),

      SetterLaws.laws(
        setter = sumPrism,
        aGen = genSum,
        bGen = Arb.string(),
        funcGen = Arb.functionAToB(Arb.string()),
      ),

      TraversalLaws.laws(
        traversal = sumPrism,
        aGen = genSum,
        bGen = Arb.string(),
        funcGen = Arb.functionAToB(Arb.string()),
      ),

      OptionalLaws.laws(
        optional = sumPrism,
        aGen = genSum,
        bGen = Arb.string(),
        funcGen = Arb.functionAToB(Arb.string()),
      )
    )

    testLaws(
      PrismLaws.laws(
        prism = sumPrism.first(),
        aGen = Gen.pair(genSum, Arb.int()),
        bGen = Gen.pair(Arb.string(), Arb.int()),
        funcGen = Arb.functionAToB(Gen.pair(Arb.string(), Arb.int())),
      )
    )

    testLaws(
      PrismLaws.laws(
        prism = sumPrism.second(),
        aGen = Gen.pair(Arb.int(), genSum),
        bGen = Gen.pair(Arb.int(), Arb.string()),
        funcGen = Arb.functionAToB(Gen.pair(Arb.int(), Arb.string())),
      )
    )

    testLaws(
      PrismLaws.laws(
        prism = sumPrism.right(),
        aGen = Gen.either(Arb.int(), genSum),
        bGen = Gen.either(Arb.int(), Arb.string()),
        funcGen = Arb.functionAToB(Gen.either(Arb.int(), Arb.string())),
      )
    )

    testLaws(
      PrismLaws.laws(
        prism = sumPrism.left(),
        aGen = Gen.either(genSum, Arb.int()),
        bGen = Gen.either(Arb.string(), Arb.int()),
        funcGen = Arb.functionAToB(Gen.either(Arb.string(), Arb.int())),
      )
    )

    testLaws(
      PrismLaws.laws(
        prism = Prism.id(),
        aGen = Gen.either(Arb.int(), Arb.int()),
        bGen = Gen.either(Arb.int(), Arb.int()),
        funcGen = Arb.functionAToB(Gen.either(Arb.int(), Arb.int())),
      )
    )

    with(sumPrism) {

      "asFold should behave as valid Fold: size" {
        checkAll(genSum) { sum: SumType ->
          size(sum) == sumPrism.getOrNull(sum)?.let { 1 } ?: 0
        }
      }

      "asFold should behave as valid Fold: nonEmpty" {
        checkAll(genSum) { sum: SumType ->
          isNotEmpty(sum) == (sumPrism.getOrNull(sum) != null)
        }
      }

      "asFold should behave as valid Fold: isEmpty" {
        checkAll(genSum) { sum: SumType ->
          isEmpty(sum) == (sumPrism.getOrNull(sum) == null)
        }
      }

      "asFold should behave as valid Fold: getAll" {
        checkAll(genSum) { sum: SumType ->
          getAll(sum) == listOfNotNull(sumPrism.getOrNull(sum))
        }
      }

      "asFold should behave as valid Fold: combineAll" {
        checkAll(genSum) { sum: SumType ->
          combineAll(Monoid.string(), sum) ==
              sumPrism.getOrNull(sum) ?: Monoid.string().empty()
        }
      }

      "asFold should behave as valid Fold: fold" {
        checkAll(genSum) { sum: SumType ->
          fold(Monoid.string(), sum) ==
              sumPrism.getOrNull(sum) ?: Monoid.string().empty()
        }
      }

      "asFold should behave as valid Fold: headOption" {
        checkAll(genSum) { sum: SumType ->
          firstOrNull(sum) == sumPrism.getOrNull(sum)
        }
      }

      "asFold should behave as valid Fold: lastOption" {
        checkAll(genSum) { sum: SumType ->
          lastOrNull(sum) == sumPrism.getOrNull(sum)
        }
      }
    }

    "Joining two prisms together with same target should yield same result" {
      checkAll(genSum) { a ->
        (sumPrism compose stringPrism).getOrNull(a) == sumPrism.getOrNull(a)?.let(stringPrism::getOrNull) &&
            (sumPrism + stringPrism).getOrNull(a) == (sumPrism compose stringPrism).getOrNull(a)
      }
    }

    "Checking if a prism exists with a target" {
      checkAll(genSum, genSum, Gen.bool()) { a, other, bool ->
        Prism.only(a) { _, _ -> bool }.isEmpty(other) == bool
      }
    }

    "Checking if there is no target" {
      checkAll(genSum) { sum ->
        sumPrism.isEmpty(sum) == sum !is SumType.A
      }
    }

    "Checking if a target exists" {
      checkAll(genSum) { sum ->
        sumPrism.isNotEmpty(sum) == sum is SumType.A
      }
    }

    "Setting a target on a prism should set the correct target" {
      checkAll(genSumTypeA, Arb.string()) { a, string ->
        (sumPrism.setNullable(a, string)!!) == a.copy(string = string)
      }
    }

    "Finding a target using a predicate within a Lens should be wrapped in the correct option result" {
      checkAll(genSum, Gen.bool()) { sum, predicate ->
        sumPrism.findOrNull(sum) { predicate }?.let { true } ?: false == (predicate && sum is SumType.A)
      }
    }

    "Checking existence predicate over the target should result in same result as predicate" {
      checkAll(genSum, Gen.bool()) { sum, predicate ->
        sumPrism.any(sum) { predicate } == (predicate && sum is SumType.A)
      }
    }

    "Checking satisfaction of predicate over the target should result in opposite result as predicate" {
      checkAll(genSum, Gen.bool()) { sum, predicate ->
        sumPrism.all(sum) { predicate } == (predicate || sum is SumType.B)
      }
    }
  }
}
