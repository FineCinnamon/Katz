package arrow.effects.internal

import arrow.Kind
import arrow.core.*
import arrow.effects.Promise
import java.util.concurrent.atomic.AtomicReference

fun <A> Promise.Companion.unsafe(): Promise<ForId, A> = UnsafePromise()

internal class UnsafePromise<A> : Promise<ForId, A> {

  private val state: AtomicReference<State<A>> = AtomicReference(State.Pending(emptyList()))

  override val get: Id<A>
    get() {
      tailrec fun loop(): Id<A> = when (val st = state.get()) {
        is State.Pending<A> -> loop()
        is State.Full -> Id(st.value)
        is State.Error -> throw st.throwable
      }

      return loop()
    }

  override val tryGet: Kind<ForId, Option<A>>
    get() = when (val oldState = state.get()) {
      is State.Pending<A> -> Id(None)
      is State.Full -> Id(Some(oldState.value))
      is State.Error -> Id(None)
    }

  override fun tryComplete(a: A): Id<Boolean> {
    tailrec fun calculateNewState(): Unit {
      val oldState = state.get()
      val newState = when (oldState) {
        is State.Pending<A> -> State.Full(a)
        is State.Full -> oldState
        is State.Error -> oldState
      }

      if (state.compareAndSet(oldState, newState)) Unit else calculateNewState()
    }

    val oldState = state.get()
    return when (oldState) {
      is State.Pending -> {
        calculateNewState()
        Id(true)
      }
      is State.Full -> Id(false)
      is State.Error -> Id(false)
    }
  }

  override fun error(throwable: Throwable): Id<Unit> =
    throw throwable

  override fun tryError(throwable: Throwable): Id<Boolean> = state.get().let { oldState ->
    when (oldState) {
      is State.Pending -> throw throwable
      is State.Full -> Id(false)
      is State.Error -> Id(false)
    }
  }

  override fun complete(a: A): Id<Unit> {
    tailrec fun calculateNewState(): Unit {
      val oldState = state.get()
      val newState = when (oldState) {
        is State.Pending<A> -> State.Full(a)
        is State.Full -> throw Promise.AlreadyFulfilled
        is State.Error -> throw Promise.AlreadyFulfilled
      }

      if (state.compareAndSet(oldState, newState)) Unit else calculateNewState()
    }

    val oldState = state.get()
    return when (oldState) {
      is State.Pending -> Id(calculateNewState())
      is State.Full -> throw Promise.AlreadyFulfilled
      is State.Error -> throw Promise.AlreadyFulfilled
    }
  }

  internal sealed class State<out A> {
    data class Pending<A>(val joiners: List<(Either<Throwable, A>) -> Unit>) : State<A>()
    data class Full<A>(val value: A) : State<A>()
    data class Error<A>(val throwable: Throwable) : State<A>()
  }

}
