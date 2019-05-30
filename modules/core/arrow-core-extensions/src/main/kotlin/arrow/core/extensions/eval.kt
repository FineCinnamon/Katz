package arrow.core.extensions

import arrow.Kind
import arrow.core.Either
import arrow.core.Eval
import arrow.core.EvalOf
import arrow.core.ForEval
import arrow.core.extensions.eval.monad.monad
import arrow.extension
import arrow.core.fix
import arrow.typeclasses.Applicative
import arrow.typeclasses.Apply
import arrow.typeclasses.Bimonad
import arrow.typeclasses.BindingStrategy
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadContext
import arrow.typeclasses.MonadContinuation
import arrow.typeclasses.MonadFx

@extension
interface EvalFunctor : Functor<ForEval> {
  override fun <A, B> EvalOf<A>.map(f: (A) -> B): Eval<B> =
    fix().map(f)
}

@extension
interface EvalApply : Apply<ForEval> {
  override fun <A, B> EvalOf<A>.ap(ff: EvalOf<(A) -> B>): Eval<B> =
    fix().ap(ff)

  override fun <A, B> EvalOf<A>.map(f: (A) -> B): Eval<B> =
    fix().map(f)
}

@extension
interface EvalApplicative : Applicative<ForEval> {
  override fun <A, B> EvalOf<A>.ap(ff: EvalOf<(A) -> B>): Eval<B> =
    fix().ap(ff)

  override fun <A, B> EvalOf<A>.map(f: (A) -> B): Eval<B> =
    fix().map(f)

  override fun <A> just(a: A): Eval<A> =
    Eval.just(a)
}

@extension
interface EvalMonad : Monad<ForEval> {
  override fun <A, B> EvalOf<A>.ap(ff: EvalOf<(A) -> B>): Eval<B> =
    fix().ap(ff)

  override fun <A, B> EvalOf<A>.flatMap(f: (A) -> EvalOf<B>): Eval<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: (A) -> EvalOf<Either<A, B>>): Eval<B> =
    Eval.tailRecM(a, f)

  override fun <A, B> EvalOf<A>.map(f: (A) -> B): Eval<B> =
    fix().map(f)

  override fun <A> just(a: A): Eval<A> =
    Eval.just(a)

  override fun <A> MonadContinuation<ForEval, *>.bindStrategy(fa: Kind<ForEval, A>): BindingStrategy<ForEval, A> =
    BindingStrategy.Strict(fa.fix().value())

  override val fx: MonadFx<ForEval>
    get() = EvalFxMonad
}

internal object EvalFxMonad : MonadFx<ForEval> {
  override val M: Monad<ForEval> = Eval.monad()
  override fun <A> monad(c: suspend MonadContext<ForEval>.() -> A): Eval<A> =
    Eval.defer { super.monad(c).fix() }
}

@extension
interface EvalComonad : Comonad<ForEval> {
  override fun <A, B> EvalOf<A>.coflatMap(f: (EvalOf<A>) -> B): Eval<B> =
    fix().coflatMap(f)

  override fun <A> EvalOf<A>.extract(): A =
    fix().extract()

  override fun <A, B> EvalOf<A>.map(f: (A) -> B): Eval<B> =
    fix().map(f)
}

@extension
interface EvalBimonad : Bimonad<ForEval> {
  override fun <A, B> EvalOf<A>.ap(ff: EvalOf<(A) -> B>): Eval<B> =
    fix().ap(ff)

  override fun <A, B> EvalOf<A>.flatMap(f: (A) -> EvalOf<B>): Eval<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, EvalOf<Either<A, B>>>): Eval<B> =
    Eval.tailRecM(a, f)

  override fun <A, B> EvalOf<A>.map(f: (A) -> B): Eval<B> =
    fix().map(f)

  override fun <A> just(a: A): Eval<A> =
    Eval.just(a)

  override fun <A, B> EvalOf<A>.coflatMap(f: (EvalOf<A>) -> B): Eval<B> =
    fix().coflatMap(f)

  override fun <A> EvalOf<A>.extract(): A =
    fix().extract()
}

fun <B> Eval.Companion.fx(c: suspend MonadContext<ForEval>.() -> B): Eval<B> =
  Eval.monad().fx.monad(c).fix()
