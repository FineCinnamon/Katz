package kategory

import io.kotlintest.matchers.shouldBe
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object MonadLaws {

    inline fun <reified F> laws(M: Monad<F> = monad<F>()): List<Law> =
            ApplicativeLaws.laws(M) + listOf(
                    Law("Monad Laws: left identity", { leftIdentity(M) }),
                    Law("Monad Laws: right identity", { rightIdentity(M) }),
                    Law("Monad Laws: kleisli left identity", { kleisliLeftIdentity(M) }),
                    Law("Monad Laws: kleisli right identity", { kleisliRightIdentity(M) }),
                    Law("Monad Laws: map / flatMap coherence", { mapFlatMapCoherence(M) }),
                    Law("Monad / JVM: stack safe", { mapFlatMapCoherence(M) })
            )

    inline fun <reified F> leftIdentity(M: Monad<F> = monad<F>()): Unit =
            forAll(genFunctionAToB<Int, HK<F, Int>>(genApplicative(Gen.int(), M)), Gen.int(), { f: (Int) -> HK<F, Int>, a: Int ->
                M.flatMap(M.pure(a), f) == f(a)
            })

    inline fun <reified F> rightIdentity(M: Monad<F> = monad<F>()): Unit =
            forAll(genApplicative(Gen.int(), M), { fa: HK<F, Int> ->
                M.flatMap(fa, { M.pure(it) }) == fa
            })

    inline fun <reified F> kleisliLeftIdentity(M: Monad<F> = monad<F>()): Unit =
            forAll(genFunctionAToB<Int, HK<F, Int>>(genApplicative(Gen.int(), M)), Gen.int(), { f: (Int) -> HK<F, Int>, a: Int ->
                (Kleisli({ n : Int -> M.pure(n)}, M) andThen Kleisli(f, M)).run(a) == f(a)
            })

    inline fun <reified F> kleisliRightIdentity(M: Monad<F> = monad<F>()): Unit =
            forAll(genFunctionAToB<Int, HK<F, Int>>(genApplicative(Gen.int(), M)), Gen.int(), { f: (Int) -> HK<F, Int>, a: Int ->
                (Kleisli(f, M) andThen Kleisli({ n : Int -> M.pure(n)}, M)).run(a) == f(a)
            })

    inline fun <reified F> mapFlatMapCoherence(M: Monad<F> = monad<F>()): Unit =
            forAll(genFunctionAToB<Int, Int>(Gen.int()), genApplicative(Gen.int(), M), { f: (Int) -> Int, fa: HK<F, Int> ->
                M.flatMap(fa, { M.pure(f(it))}) == M.map(fa, f)
            })

    inline fun <reified F> stackSafety(iterations : Int = 5000, M: Monad<F> = monad<F>()): Unit {
        val res = M.tailRecM(0, { i -> M.pure(if (i < iterations) Either.Left(i + 1) else Either.Right(i))})
        res shouldBe iterations
    }

}
