package kategory

import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.forAll
import kategory.Either.Left
import kategory.Either.Right
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class EitherTest : UnitSpec() {
    init {

        testLaws(MonadErrorLaws.laws(Either.monadError(), Eq.any(), Eq.any()))
        testLaws(TraverseLaws.laws(Either.traverse<Throwable>(), Either.applicative(), { it.right() }, Eq.any()))
        testLaws(SemigroupKLaws.laws(
                Either.semigroupK(),
                Either.applicative(),
                Eq<HK<EitherKindPartial<IdHK>, Int>> { a, b ->
                    a.ev() == b.ev()
                }))

        "getOrElse should return value" {
            forAll { a: Int, b: Int ->
                Right(a).getOrElse { b } == a
                        && Left(a).getOrElse { b } == b
            }

        }

        "filterOrElse should filters value" {
            forAll { a: Int, b: Int ->
                val left: Either<Int, Int> = Left(a)

                Right(a).filterOrElse({ it > a - 1 }, { b }) == Right(a)
                        && Right(a).filterOrElse({ it > a + 1 }, { b }) == Left(b)
                        && left.filterOrElse({ it > a - 1 }, { b }) == Left(a)
                        && left.filterOrElse({ it > a + 1 }, { b }) == Left(a)
            }
        }

        "swap should interchange values" {
            forAll { a: Int ->
                Left(a).swap() == Right(a)
                        && Right(a).swap() == Left(a)
            }
        }

        "toOption should convert" {
            forAll { a: Int ->
                Right(a).toOption() == Option.Some(a)
                        && Left(a).toOption() == Option.None
            }
        }

        "contains should check value" {
            forAll { a: Int, b: Int ->
                Right(a).contains(a)
                        && !Right(a).contains(b)
                        && !Left(a).contains(a)
            }
        }

    }
}
