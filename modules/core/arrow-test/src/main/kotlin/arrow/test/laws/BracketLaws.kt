package arrow.test.laws

import arrow.Kind
import arrow.core.Either
import arrow.effects.typeclasses.Bracket
import arrow.effects.typeclasses.ExitCase
import arrow.test.generators.genApplicative
import arrow.test.generators.genFunctionAToB
import arrow.test.generators.genThrowable
import arrow.typeclasses.Eq
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object BracketLaws {
  fun <F> laws(
    BF: Bracket<F, Throwable>,
    EQ: Eq<Kind<F, Int>>,
    EQ_EITHER: Eq<Kind<F, Either<Throwable, Int>>>,
    EQERR: Eq<Kind<F, Int>> = EQ
  ): List<Law> =
    MonadErrorLaws.laws(BF, EQERR, EQ_EITHER, EQ) + listOf(
      Law("Bracket: bracketCase with just Unit is eqv to Map") { BF.bracketCaseWithJustUnitEqvMap(EQ) },
      Law("Bracket: bracketCase with just Unit is uncancelable") { BF.bracketCaseWithJustUnitIsUncancelable(EQ) },
      Law("Bracket: bracketCase failure in acquisition remains failure") { BF.bracketCaseFailureInAcquisitionRemainsFailure(EQ) },
      Law("Bracket: bracket is derived from bracketCase") { BF.bracketIsDerivedFromBracketCase(EQ) },
      Law("Bracket: uncancelable prevents Cancelled case") { BF.uncancelablePreventsCanceledCase(BF.just(Unit), BF.just(Unit), EQ) },
      Law("Bracket: acquire and release are uncancelable") { BF.acquireAndReleaseAreUncancelable({ BF.just(Unit) }, EQ) },
      Law("Bracket: guarantee is derived from bracket") { BF.guaranteeIsDerivedFromBracket(BF.just(Unit), EQ) },
      Law("Bracket: guaranteeCase is derived from bracketCase") { BF.guaranteeCaseIsDerivedFromBracketCase({ BF.just(Unit) }, EQ) }
    )

  fun <F> Bracket<F, Throwable>.bracketCaseWithJustUnitEqvMap(EQ: Eq<Kind<F, Int>>): Unit =
    forAll(genApplicative(Gen.int(), this), genFunctionAToB(Gen.int())
    ) { fa: Kind<F, Int>, f: (Int) -> Int ->
      fa.bracketCase(release = { _, _ -> just(Unit) }, use = { a -> just(f(a)) }).equalUnderTheLaw(fa.map(f), EQ)
    }

  fun <F> Bracket<F, Throwable>.bracketCaseWithJustUnitIsUncancelable(
    EQ: Eq<Kind<F, Int>>): Unit =
    forAll(genApplicative(Gen.int(), this)) { fa: Kind<F, Int> ->
      fa.bracketCase(release = { _, _ -> just(Unit) }, use = { just(it) }).equalUnderTheLaw(fa.uncancelable().flatMap { just(it) }, EQ)
    }

  fun <F> Bracket<F, Throwable>.bracketCaseFailureInAcquisitionRemainsFailure(
    EQ: Eq<Kind<F, Int>>): Unit =
    forAll(genThrowable()) { e ->
      raiseError<Int>(e).bracketCase(release = { _, _ -> just(Unit) }, use = { just(it) }).equalUnderTheLaw(raiseError(e), EQ)
    }

  fun <F> Bracket<F, Throwable>.bracketIsDerivedFromBracketCase(
    EQ: Eq<Kind<F, Int>>): Unit =
    forAll(genApplicative(Gen.int(), this)) { fa: Kind<F, Int> ->
      fa.bracket(release = { _ -> just(Unit) }, use = { just(it) }).equalUnderTheLaw(fa.bracketCase(release = { _, _ -> just(Unit) }, use = { just(it) }), EQ)
    }

  fun <F> Bracket<F, Throwable>.uncancelablePreventsCanceledCase(
    onCancel: Kind<F, Unit>,
    onFinish: Kind<F, Unit>,
    EQ: Eq<Kind<F, Int>>): Unit =
    forAll(genApplicative(Gen.int(), this)) { fa: Kind<F, Int> ->
      just(Unit).bracketCase(use = { fa }, release = { _, b ->
        if (b == ExitCase.Cancelled) onCancel else onFinish
      }).uncancelable().equalUnderTheLaw(fa.guarantee(onFinish), EQ)
    }

  fun <F> Bracket<F, Throwable>.acquireAndReleaseAreUncancelable(
    release: (Int) -> Kind<F, Unit>,
    EQ: Eq<Kind<F, Int>>): Unit =
    forAll(genApplicative(Gen.int(), this)) { fa: Kind<F, Int> ->
      fa.uncancelable().bracket({ just(it) }) { a -> release(a).uncancelable() }.equalUnderTheLaw(fa.bracket({ just(it) }, release), EQ)
    }

  fun <F> Bracket<F, Throwable>.guaranteeIsDerivedFromBracket(
    finalizer: Kind<F, Unit>,
    EQ: Eq<Kind<F, Int>>): Unit =
    forAll(genApplicative(Gen.int(), this)) { fa: Kind<F, Int> ->
      fa.guarantee(finalizer).equalUnderTheLaw(just(Unit).bracket({ fa }, { finalizer }), EQ)
    }

  fun <F> Bracket<F, Throwable>.guaranteeCaseIsDerivedFromBracketCase(
    finalizer: (ExitCase<Throwable>) -> Kind<F, Unit>,
    EQ: Eq<Kind<F, Int>>): Unit =
    forAll(genApplicative(Gen.int(), this)) { fa: Kind<F, Int> ->
      fa.guaranteeCase(finalizer).equalUnderTheLaw(just(Unit).bracketCase({ fa }, { _, e -> finalizer(e) }), EQ)
    }
}
