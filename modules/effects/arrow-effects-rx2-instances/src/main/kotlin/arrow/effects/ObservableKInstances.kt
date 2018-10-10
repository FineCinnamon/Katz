package arrow.effects

import arrow.Kind
import arrow.core.Either
import arrow.core.Eval
import arrow.effects.syntax.observablek.monad.monad
import arrow.effects.syntax.observablek.monadError.monadError
import arrow.effects.typeclasses.*
import arrow.instance
import arrow.typeclasses.*
import kotlin.coroutines.experimental.CoroutineContext

@instance
interface ObservableKFunctorInstance : Functor<ForObservableK> {
  override fun <A, B> Kind<ForObservableK, A>.map(f: (A) -> B): ObservableK<B> =
    fix().map(f)
}

@instance
interface ObservableKApplicativeInstance : Applicative<ForObservableK> {
  override fun <A, B> ObservableKOf<A>.ap(ff: ObservableKOf<(A) -> B>): ObservableK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForObservableK, A>.map(f: (A) -> B): ObservableK<B> =
    fix().map(f)

  override fun <A> just(a: A): ObservableK<A> =
    ObservableK.just(a)
}

@instance
interface ObservableKMonadInstance : Monad<ForObservableK> {
  override fun <A, B> ObservableKOf<A>.ap(ff: ObservableKOf<(A) -> B>): ObservableK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForObservableK, A>.flatMap(f: (A) -> Kind<ForObservableK, B>): ObservableK<B> =
    fix().flatMap(f)

  override fun <A, B> Kind<ForObservableK, A>.map(f: (A) -> B): ObservableK<B> =
    fix().map(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, ObservableKOf<arrow.core.Either<A, B>>>): ObservableK<B> =
    ObservableK.tailRecM(a, f)

  override fun <A> just(a: A): ObservableK<A> =
    ObservableK.just(a)
}

@instance
interface ObservableKFoldableInstance : Foldable<ForObservableK> {
  override fun <A, B> Kind<ForObservableK, A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> Kind<ForObservableK, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): arrow.core.Eval<B> =
    fix().foldRight(lb, f)
}

@instance
interface ObservableKTraverseInstance : Traverse<ForObservableK> {
  override fun <A, B> Kind<ForObservableK, A>.map(f: (A) -> B): ObservableK<B> =
    fix().map(f)

  override fun <G, A, B> ObservableKOf<A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, ObservableK<B>> =
    fix().traverse(AP, f)

  override fun <A, B> Kind<ForObservableK, A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> Kind<ForObservableK, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): arrow.core.Eval<B> =
    fix().foldRight(lb, f)
}

@instance
interface ObservableKApplicativeErrorInstance :
  ApplicativeError<ForObservableK, Throwable>,
  ObservableKApplicativeInstance {
  override fun <A> raiseError(e: Throwable): ObservableK<A> =
    ObservableK.raiseError(e)

  override fun <A> ObservableKOf<A>.handleErrorWith(f: (Throwable) -> ObservableKOf<A>): ObservableK<A> =
    fix().handleErrorWith { f(it).fix() }
}

@instance
interface ObservableKMonadErrorInstance :
  MonadError<ForObservableK, Throwable>,
  ObservableKMonadInstance {
  override fun <A> raiseError(e: Throwable): ObservableK<A> =
    ObservableK.raiseError(e)

  override fun <A> ObservableKOf<A>.handleErrorWith(f: (Throwable) -> ObservableKOf<A>): ObservableK<A> =
    fix().handleErrorWith { f(it).fix() }
}

@instance
interface ObservableKMonadDeferInstance :
  MonadDefer<ForObservableK>,
  ObservableKMonadErrorInstance {
  override fun <A> defer(fa: () -> ObservableKOf<A>): ObservableK<A> =
    ObservableK.defer(fa)
}

@instance
interface ObservableKAsyncInstance :
  Async<ForObservableK>,
  ObservableKMonadDeferInstance {
  override fun <A> async(fa: Proc<A>): ObservableK<A> =
    ObservableK.runAsync(fa)

  override fun <A> ObservableKOf<A>.continueOn(ctx: CoroutineContext): ObservableK<A> =
    fix().continueOn(ctx)
}

@instance
interface ObservableKEffectInstance :
  Effect<ForObservableK>,
  ObservableKAsyncInstance {
  override fun <A> ObservableKOf<A>.runAsync(cb: (Either<Throwable, A>) -> ObservableKOf<Unit>): ObservableK<Unit> =
    fix().runAsync(cb)
}

@instance
interface ObservableKConcurrentEffectInstance : ConcurrentEffect<ForObservableK>, ObservableKEffectInstance {
  override fun <A> Kind<ForObservableK, A>.runAsyncCancellable(cb: (Either<Throwable, A>) -> ObservableKOf<Unit>): ObservableK<Disposable> =
    fix().runAsyncCancellable(cb)
}

fun ObservableK.Companion.monadFlat(): ObservableKMonadInstance = monad()

fun ObservableK.Companion.monadConcat(): ObservableKMonadInstance = object : ObservableKMonadInstance {
  override fun <A, B> Kind<ForObservableK, A>.flatMap(f: (A) -> Kind<ForObservableK, B>): ObservableK<B> =
    fix().concatMap { f(it).fix() }
}

fun ObservableK.Companion.monadSwitch(): ObservableKMonadInstance = object : ObservableKMonadErrorInstance {
  override fun <A, B> Kind<ForObservableK, A>.flatMap(f: (A) -> Kind<ForObservableK, B>): ObservableK<B> =
    fix().switchMap { f(it).fix() }
}

fun ObservableK.Companion.monadErrorFlat(): ObservableKMonadErrorInstance = monadError()

fun ObservableK.Companion.monadErrorConcat(): ObservableKMonadErrorInstance = object : ObservableKMonadErrorInstance {
  override fun <A, B> Kind<ForObservableK, A>.flatMap(f: (A) -> Kind<ForObservableK, B>): ObservableK<B> =
    fix().concatMap { f(it).fix() }
}

fun ObservableK.Companion.monadErrorSwitch(): ObservableKMonadErrorInstance = object : ObservableKMonadErrorInstance {
  override fun <A, B> Kind<ForObservableK, A>.flatMap(f: (A) -> Kind<ForObservableK, B>): ObservableK<B> =
    fix().switchMap { f(it).fix() }
}

object ObservableKContext : ObservableKConcurrentEffectInstance, ObservableKTraverseInstance {
  override fun <A, B> Kind<ForObservableK, A>.map(f: (A) -> B): ObservableK<B> =
    fix().map(f)
}

infix fun <A> ForObservableK.Companion.extensions(f: ObservableKContext.() -> A): A =
  f(ObservableKContext)
