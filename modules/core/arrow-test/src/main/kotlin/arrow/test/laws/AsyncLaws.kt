package arrow.test.laws

import arrow.Kind
import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.effects.typeclasses.Async
import arrow.test.generators.genThrowable
import arrow.typeclasses.Eq
import arrow.typeclasses.binding
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import kotlinx.coroutines.experimental.newSingleThreadContext

object AsyncLaws {
  inline fun <F> laws(AC: Async<F>, EQ: Eq<Kind<F, Int>>, EQ_EITHER: Eq<Kind<F, Either<Throwable, Int>>>, EQERR: Eq<Kind<F, Int>> = EQ): List<Law> =
    MonadSuspendLaws.laws(AC, EQERR, EQ_EITHER, EQ) + listOf(
      Law("Async Laws: success equivalence", { AC.asyncSuccess(EQ) }),
      Law("Async Laws: error equivalence", { AC.asyncError(EQERR) }),
      Law("Async Laws: continueOn jumps threads", { AC.continueOn(EQ) }),
      Law("Async Laws: continueOn on comprehensions", { AC.continueOnComprehension(EQ) })
    )

  fun <F> Async<F>.asyncSuccess(EQ: Eq<Kind<F, Int>>): Unit =
    forAll(Gen.int(), { num: Int ->
      async { ff: (Either<Throwable, Int>) -> Unit -> ff(Right(num)) }.equalUnderTheLaw(just<Int>(num), EQ)
    })

  fun <F> Async<F>.asyncError(EQ: Eq<Kind<F, Int>>): Unit =
    forAll(genThrowable(), { e: Throwable ->
      async { ff: (Either<Throwable, Int>) -> Unit -> ff(Left(e)) }.equalUnderTheLaw(raiseError<Int>(e), EQ)
    })

  fun <F> Async<F>.continueOn(EQ: Eq<Kind<F, Int>>): Unit =
    forFew(5, Gen.int(), { threadId: Int ->
      Unit.just().continueOn(newSingleThreadContext(threadId.toString()))
        // Turns out that kotlinx.coroutines decides to rewrite thread names
        .map { Thread.currentThread().name.substringBefore(' ').toInt() }
        .equalUnderTheLaw(just(threadId), EQ)
    })

  fun <F> Async<F>.continueOnComprehension(EQ: Eq<Kind<F, Int>>): Unit =
    forFew(5, Gen.int(), { threadId: Int ->
      binding {
        continueOn(newSingleThreadContext(threadId.toString()))
        // Turns out that kotlinx.coroutines decides to rewrite thread names
        Thread.currentThread().name.substringBefore(' ').toInt()
      }.equalUnderTheLaw(just(threadId), EQ)
    })
}
