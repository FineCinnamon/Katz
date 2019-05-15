package arrow.effects.typeclasses

import arrow.Kind
import arrow.core.Either
import arrow.core.Tuple2
import arrow.core.toT
import arrow.effects.Ref
import arrow.effects.data.internal.BindingCancellationException
import arrow.typeclasses.MonadContinuation
import arrow.typeclasses.MonadError
import arrow.typeclasses.MonadErrorContinuation
import arrow.typeclasses.MonadThrow
import kotlin.coroutines.startCoroutine

/**
 * ank_macro_hierarchy(arrow.effects.typeclasses.MonadDefer)
 *
 * The context required to defer evaluating a safe computation.
 **/
interface MonadDefer<F> : MonadThrow<F>, Bracket<F, Throwable> {

  fun <A> defer(fa: () -> Kind<F, A>): Kind<F, A>

  fun <A> delay(f: () -> A): Kind<F, A> =
    defer {
      try {
        just(f())
      } catch (t: Throwable) {
        t.raiseNonFatal<A>()
      }
    }

  fun <A> delay(fa: Kind<F, A>): Kind<F, A> = defer { fa }

  fun lazy(): Kind<F, Unit> = delay { }

  fun <A> delayOrRaise(f: () -> Either<Throwable, A>): Kind<F, A> =
    defer { f().fold({ raiseError<A>(it) }, { just(it) }) }

  /**
   * Creates a [Ref] to purely manage mutable state, initialized by the function [f]
   */
  fun <A> ref(f: () -> A): Kind<F, Ref<F, A>> = Ref(this, f)

  /**
   * Entry point for monad bindings which enables for comprehensions. The underlying impl is based on coroutines.
   * A coroutines is initiated and inside [MonadDeferCancellableContinuation] suspended yielding to [Monad.flatMap]. Once all the flatMap binds are completed
   * the underlying monad is returned from the act of executing the coroutine
   *
   * This one operates over [MonadError] instances that can support [Throwable] in their error type automatically lifting
   * errors as failed computations in their monadic context and not letting exceptions thrown as the regular monad binding does.
   *
   * This operation is cancellable by calling invoke on the [Disposable] return.
   * If [Disposable.invoke] is called the binding result will become a lifted [BindingCancellationException].
   */
  @Deprecated(
    "`bindingCancellable` is getting renamed to `fxCancellable` for consistency with the Arrow IO system. Use the IO extensions for comprehensions",
    ReplaceWith("fxCancellable")
  )
  fun <B> bindingCancellable(c: suspend MonadDeferCancellableContinuation<F, *>.() -> B): Tuple2<Kind<F, B>, Disposable> {
    val continuation = MonadDeferCancellableContinuation<F, B>(this)
    val wrapReturn: suspend MonadDeferCancellableContinuation<F, *>.() -> Kind<F, B> = { just(c()) }
    wrapReturn.startCoroutine(continuation, continuation)
    return continuation.returnedMonad() toT continuation.disposable()
  }

  override fun <B> binding(c: suspend MonadContinuation<F, *>.() -> B): Kind<F, B> =
    bindingCancellable { c() }.a

  override fun <B> bindingCatch(c: suspend MonadErrorContinuation<F, *>.() -> B): Kind<F, B> =
    bindingCancellable { c() }.a
}
