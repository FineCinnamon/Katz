package arrow.optics

import arrow.core.Either
import arrow.core.OptionMonoidInstanceImplicits
import arrow.core.applicative
import arrow.core.ev
import arrow.instances.IntMonoid
import arrow.optics.instances.nonePrism
import arrow.optics.instances.nullableToOption
import arrow.optics.instances.optionToEither
import arrow.optics.instances.somePrism
import arrow.syntax.either.right
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.Gen
import org.junit.runner.RunWith
import arrow.test.UnitSpec
import arrow.test.generators.genEither
import arrow.test.generators.genFunctionAToB
import arrow.test.generators.genNullable
import arrow.test.generators.genOption
import arrow.test.laws.IsoLaws
import arrow.test.laws.PrismLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.Monoid

@RunWith(KTestJUnitRunner::class)
class OptionInstancesTest : UnitSpec() {

    init {

        testLaws(
            IsoLaws.laws(
                iso = nullableToOption<Int>(),
                aGen = genNullable(Gen.int()),
                bGen = genOption(Gen.int()),
                EQA = Eq.any(),
                EQB = Eq.any(),
                funcGen = genFunctionAToB(genOption(Gen.int())),
                bMonoid = OptionMonoidInstanceImplicits.instance(IntMonoid)),

            PrismLaws.laws(
                prism = somePrism(),
                aGen = genOption(Gen.int()),
                bGen = Gen.int(),
                funcGen = genFunctionAToB(Gen.int()),
                EQA = Eq.any(),
                EQB = Eq.any(),
                EQOptionB = Eq.any()),

            PrismLaws.laws(
                prism = nonePrism(),
                aGen = genOption(Gen.int()),
                bGen = Gen.create { Unit },
                funcGen = genFunctionAToB(Gen.create { Unit }),
                EQA = Eq.any(),
                EQB = Eq.any(),
                EQOptionB = Eq.any()),

            IsoLaws.laws(
                iso = optionToEither(),
                aGen = genOption(Gen.int()),
                bGen = genEither(Gen.create { Unit }, Gen.int()),
                funcGen = genFunctionAToB(genEither(Gen.create { Unit }, Gen.int())),
                EQA = Eq.any(),
                EQB = Eq.any(),
                bMonoid = object : Monoid<Either<Unit, Int>> {
                    override fun combine(a: Either<Unit, Int>, b: Either<Unit, Int>): Either<Unit, Int> =
                            Either.applicative<Unit>().map2(a, b) { (a, b) -> a + b }.ev()

                    override fun empty(): Either<Unit, Int> = 0.right()
                })
        )

    }
}
