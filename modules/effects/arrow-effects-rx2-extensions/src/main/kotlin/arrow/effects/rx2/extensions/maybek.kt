package arrow.effects.rx2.extensions

import arrow.core.Either
import arrow.core.Eval
import arrow.effects.rx2.ForMaybeK
import arrow.effects.rx2.MaybeK
import arrow.effects.rx2.MaybeKOf
import arrow.effects.rx2.fix
import arrow.effects.typeclasses.Async
import arrow.effects.typeclasses.Bracket
import arrow.effects.typeclasses.Duration
import arrow.effects.typeclasses.Effect
import arrow.effects.typeclasses.ExitCase
import arrow.effects.typeclasses.MonadDefer
import arrow.effects.typeclasses.Proc
import arrow.effects.typeclasses.ProcF
import arrow.effects.Timer
import arrow.effects.rx2.extensions.flowablek.async.async
import arrow.effects.rx2.extensions.maybek.async.async
import arrow.effects.typeclasses.AsyncSyntax
import arrow.extension
import arrow.typeclasses.Applicative
import arrow.typeclasses.ApplicativeError
import arrow.typeclasses.Foldable
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadError
import arrow.typeclasses.MonadThrow
import io.reactivex.Maybe
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

@extension
interface MaybeKFunctor : Functor<ForMaybeK> {
  override fun <A, B> MaybeKOf<A>.map(f: (A) -> B): MaybeK<B> =
    fix().map(f)
}

@extension
interface MaybeKApplicative : Applicative<ForMaybeK> {
  override fun <A, B> MaybeKOf<A>.ap(ff: MaybeKOf<(A) -> B>): MaybeK<B> =
    fix().ap(ff)

  override fun <A, B> MaybeKOf<A>.map(f: (A) -> B): MaybeK<B> =
    fix().map(f)

  override fun <A> just(a: A): MaybeK<A> =
    MaybeK.just(a)
}

@extension
interface MaybeKMonad : Monad<ForMaybeK> {
  override fun <A, B> MaybeKOf<A>.ap(ff: MaybeKOf<(A) -> B>): MaybeK<B> =
    fix().ap(ff)

  override fun <A, B> MaybeKOf<A>.flatMap(f: (A) -> MaybeKOf<B>): MaybeK<B> =
    fix().flatMap(f)

  override fun <A, B> MaybeKOf<A>.map(f: (A) -> B): MaybeK<B> =
    fix().map(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, MaybeKOf<Either<A, B>>>): MaybeK<B> =
    MaybeK.tailRecM(a, f)

  override fun <A> just(a: A): MaybeK<A> =
    MaybeK.just(a)
}

@extension
interface MaybeKFoldable : Foldable<ForMaybeK> {

  override fun <A, B> MaybeKOf<A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> MaybeKOf<A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    fix().foldRight(lb, f)

  override fun <A> MaybeKOf<A>.isEmpty(): Boolean =
    fix().isEmpty()

  override fun <A> MaybeKOf<A>.exists(p: (A) -> Boolean): Boolean =
    fix().exists(p)

  override fun <A> MaybeKOf<A>.forAll(p: (A) -> Boolean): Boolean =
    fix().forall(p)

  override fun <A> MaybeKOf<A>.nonEmpty(): Boolean =
    fix().nonEmpty()
}

@extension
interface MaybeKApplicativeError :
  ApplicativeError<ForMaybeK, Throwable>,
  MaybeKApplicative {
  override fun <A> raiseError(e: Throwable): MaybeK<A> =
    MaybeK.raiseError(e)

  override fun <A> MaybeKOf<A>.handleErrorWith(f: (Throwable) -> MaybeKOf<A>): MaybeK<A> =
    fix().handleErrorWith { f(it).fix() }
}

@extension
interface MaybeKMonadError :
  MonadError<ForMaybeK, Throwable>,
  MaybeKMonad {
  override fun <A> raiseError(e: Throwable): MaybeK<A> =
    MaybeK.raiseError(e)

  override fun <A> MaybeKOf<A>.handleErrorWith(f: (Throwable) -> MaybeKOf<A>): MaybeK<A> =
    fix().handleErrorWith { f(it).fix() }
}

@extension
interface MaybeKMonadThrow : MonadThrow<ForMaybeK>, MaybeKMonadError

@extension
interface MaybeKBracket : Bracket<ForMaybeK, Throwable>, MaybeKMonadThrow {
  override fun <A, B> MaybeKOf<A>.bracketCase(release: (A, ExitCase<Throwable>) -> MaybeKOf<Unit>, use: (A) -> MaybeKOf<B>): MaybeK<B> =
    fix().bracketCase({ use(it) }, { a, e -> release(a, e) })
}

@extension
interface MaybeKMonadDefer : MonadDefer<ForMaybeK>, MaybeKBracket {
  override fun <A> defer(fa: () -> MaybeKOf<A>): MaybeK<A> =
    MaybeK.defer(fa)
}

@extension
interface MaybeKAsync : Async<ForMaybeK>, MaybeKMonadDefer {
  override fun <A> async(fa: Proc<A>): MaybeK<A> =
    MaybeK.async { _, cb -> fa(cb) }

  override fun <A> asyncF(k: ProcF<ForMaybeK, A>): MaybeK<A> =
    MaybeK.asyncF { _, cb -> k(cb) }

  override fun <A> MaybeKOf<A>.continueOn(ctx: CoroutineContext): MaybeK<A> =
    fix().continueOn(ctx)
}

@extension
interface MaybeKEffect :
  Effect<ForMaybeK>,
  MaybeKAsync {
  override fun <A> MaybeKOf<A>.runAsync(cb: (Either<Throwable, A>) -> MaybeKOf<Unit>): MaybeK<Unit> =
    fix().runAsync(cb)
}

@extension
interface MaybeKTimer : Timer<ForMaybeK> {
  override fun sleep(duration: Duration): MaybeK<Unit> =
    MaybeK(Maybe.timer(duration.nanoseconds, TimeUnit.NANOSECONDS)
      .map { Unit })
}

// TODO MaybeK does not yet have a Concurrent instance
fun <A> MaybeK.Companion.fx(c: suspend AsyncSyntax<ForMaybeK>.() -> A): MaybeK<A> =
  MaybeK.async().fx.async(c).fix()
