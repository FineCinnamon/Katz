package arrow.effects.typeclasses

import arrow.Kind
import arrow.core.Either
import arrow.effects.data.internal.BindingCancellationException
import arrow.typeclasses.MonadErrorContinuation
import arrow.typeclasses.bindingCatch
import arrow.typeclasses.stackLabels
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.RestrictsSuspension
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn
import kotlin.coroutines.experimental.startCoroutine

typealias Disposable = () -> Unit

@RestrictsSuspension
open class MonadDeferCancellableContinuation<F, A>(SC: MonadDefer<F>, override val context: CoroutineContext = EmptyCoroutineContext) :
  MonadErrorContinuation<F, A>(SC), MonadDefer<F> by SC {

  protected val cancelled: AtomicBoolean = AtomicBoolean(false)

  fun disposable(): Disposable = { cancelled.set(true) }

  override fun returnedMonad(): Kind<F, A> = returnedMonad

  suspend fun <B> bindDefer(f: () -> B): B =
    invoke(f).bind()

  suspend fun <B> bindDeferIn(context: CoroutineContext, f: () -> B): B =
    defer { bindingCatch { bindIn(context, f) } }.bind()

  suspend fun <B> bindDeferUnsafe(f: () -> Either<Throwable, B>): B =
    deferUnsafe(f).bind()

  override suspend fun <B> bind(m: () -> Kind<F, B>): B = suspendCoroutineOrReturn { c ->
    val labelHere = c.stackLabels // save the whole coroutine stack labels
    returnedMonad = m().flatMap({ x: B ->
      c.stackLabels = labelHere
      if (cancelled.get()) {
        throw BindingCancellationException()
      }
      c.resume(x)
      returnedMonad
    })
    COROUTINE_SUSPENDED
  }

  override suspend fun <B> bindIn(context: CoroutineContext, m: () -> B): B = suspendCoroutineOrReturn { c ->
    val labelHere = c.stackLabels // save the whole coroutine stack labels
    val monadCreation: suspend () -> Kind<F, A> = {
      val datatype = try {
        just(m())
      } catch (t: Throwable) {
        ME.raiseError<B>(t)
      }
      datatype.flatMap({ xx: B ->
        c.stackLabels = labelHere
        if (cancelled.get()) {
          throw BindingCancellationException()
        }
        c.resume(xx)
        returnedMonad
      })
    }
    val completion = bindingInContextContinuation(context)
    returnedMonad = just(Unit).flatMap({
      monadCreation.startCoroutine(completion)
      val error = completion.await()
      if (error != null) {
        throw error
      }
      returnedMonad
    })
    COROUTINE_SUSPENDED
  }
}
