package arrow.optics

import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.Gen
import arrow.core.Either
import arrow.core.applicative
import arrow.core.ev
import arrow.typeclasses.Eq
import arrow.test.laws.IsoLaws
import arrow.typeclasses.Monoid
import arrow.data.Try
import arrow.data.applicative
import arrow.data.ev
import arrow.test.UnitSpec
import arrow.test.generators.genEither
import arrow.test.generators.genFunctionAToB
import arrow.test.generators.genThrowable
import arrow.test.generators.genTry
import arrow.test.generators.genValidated
import arrow.optics.instances.validatedToEither
import arrow.optics.instances.validatedToTry
import arrow.syntax.either.right
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class ValidatedInstancesTest : UnitSpec() {

    init {

        testLaws(
            IsoLaws.laws(
                iso = validatedToEither(),
                aGen = genValidated(Gen.string(), Gen.int()),
                bGen = genEither(Gen.string(), Gen.int()),
                funcGen = genFunctionAToB(genEither(Gen.string(), Gen.int())),
                EQA = Eq.any(),
                EQB = Eq.any(),
                bMonoid = object : Monoid<Either<String, Int>> {
                    override fun empty() = 0.right()

                    override fun combine(a: Either<String, Int>, b: Either<String, Int>): Either<String, Int> =
                            Either.applicative<String>().map2(a, b) { (a, b) -> a + b }.ev()
                }),

            IsoLaws.laws(
                iso = validatedToTry(),
                aGen = genValidated(genThrowable(), Gen.int()),
                bGen = genTry(Gen.int()),
                funcGen = genFunctionAToB(genTry(Gen.int())),
                EQA = Eq.any(),
                EQB = Eq.any(),
                bMonoid = object : Monoid<Try<Int>> {
                    override fun combine(a: Try<Int>, b: Try<Int>) = Try.applicative().map2(a, b) { (a, b) -> a + b }.ev()

                    override fun empty(): Try<Int> = Try.Success(0)
                })
        )

    }

}
