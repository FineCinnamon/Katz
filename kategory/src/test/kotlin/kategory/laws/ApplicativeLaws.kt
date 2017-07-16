package kategory

import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object ApplicativeLaws {

    inline fun <reified F> laws(A: Applicative<F> = applicative<F>(), EQ: Eq<HK<F, Int>>): List<Law> =
            FunctorLaws.laws(A, EQ) + listOf(
                    Law("Applicative Laws: ap identity", { apIdentity(A, EQ) }),
                    Law("Applicative Laws: homomorphism", { homomorphism(A, EQ) }),
                    Law("Applicative Laws: interchange", { interchange(A, EQ) }),
                    Law("Applicative Laws: map derived", { mapDerived(A, EQ) })
            )

    inline fun <reified F> apIdentity(A: Applicative<F> = applicative<F>(), EQ: Eq<HK<F, Int>>): Unit =
            forAll(genApplicative(Gen.int(), A), { fa: HK<F, Int> ->
                A.ap(fa, A.pure({ n: Int -> n })).equalUnderTheLaw(fa, EQ)
            })

    inline fun <reified F> homomorphism(A: Applicative<F> = applicative<F>(), EQ: Eq<HK<F, Int>>): Unit =
            forAll(genFunctionAToB<Int, Int>(Gen.int()), Gen.int(), { ab: (Int) -> Int, a: Int ->
                A.ap(A.pure(a), A.pure(ab)).equalUnderTheLaw(A.pure(ab(a)), EQ)
            })

    inline fun <reified F> interchange(A: Applicative<F> = applicative<F>(), EQ: Eq<HK<F, Int>>): Unit =
            forAll(genApplicative(genFunctionAToB<Int, Int>(Gen.int()), A), Gen.int(), { fa: HK<F, (Int) -> Int>, a: Int ->
                A.ap(A.pure(a), fa).equalUnderTheLaw(A.ap(fa, A.pure({ x: (Int) -> Int -> x(a) })), EQ)
            })

    inline fun <reified F> mapDerived(A: Applicative<F> = applicative<F>(), EQ: Eq<HK<F, Int>>): Unit =
            forAll(genApplicative(Gen.int(), A), genFunctionAToB<Int, Int>(Gen.int()), { fa: HK<F, Int>, f: (Int) -> Int ->
                A.map(fa, f).equalUnderTheLaw(A.ap(fa, A.pure(f)), EQ)
            })

}
