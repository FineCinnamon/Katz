package arrow.effects.suspended.fx

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.Tuple3
import arrow.effects.internal.Platform
import arrow.effects.internal.UnsafePromise
import arrow.effects.internal.asyncContinuation
import arrow.effects.typeclasses.RaceTriple
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

fun <A, B, C> Fx.Companion.raceTriple(ctx: CoroutineContext, fa: FxOf<A>, fb: FxOf<B>, fc: FxOf<C>): Fx<RaceTriple<ForFx, A, B, C>> = async { conn, cb ->
  val active = AtomicBoolean(true)

  val upstreamCancelToken = defer { if (conn.isCanceled()) unit else conn.cancel() }

  val connA = FxConnection()
  connA.push(upstreamCancelToken)
  val promiseA = UnsafePromise<A>()

  val connB = FxConnection()
  connB.push(upstreamCancelToken)
  val promiseB = UnsafePromise<B>()

  val connC = FxConnection()
  connC.push(upstreamCancelToken)
  val promiseC = UnsafePromise<C>()

  conn.push(connA.cancel(), connB.cancel(), connC.cancel())

  suspend {
    suspendCoroutine { ca: Continuation<A> ->
      FxRunLoop.startCancelable(fa, connA, ctx) { either: Either<Throwable, A> ->
        either.fold({ error ->
          ca.resumeWith(Result.failure(error))
        }, { a ->
          ca.resumeWith(Result.success(a))
        })
      }
    }
  }.startCoroutine(asyncContinuation(ctx) { either ->
    either.fold({ error ->
      if (active.getAndSet(false)) { // if an error finishes first, stop the race.
        FxRunLoop.start(connB.cancel()) { r2 ->
          FxRunLoop.start(connC.cancel()) { r3 ->
            conn.pop()
            val errorResult = r2.fold(ifLeft = { e2 ->
              r3.fold(ifLeft = { e3 -> Platform.composeErrors(error, e2, e3) }, ifRight = { Platform.composeErrors(error, e2) })
            }, ifRight = {
              r3.fold(ifLeft = { e3 -> Platform.composeErrors(error, e3) }, ifRight = { error })
            })
            cb(Left(errorResult))
          }
        }
      } else {
        promiseA.complete(Left(error))
      }
    }, { a ->
      if (active.getAndSet(false)) {
        conn.pop()
        cb(Right(Left(Tuple3(a, FxFiber(promiseB, connB), FxFiber(promiseC, connC)))))
      } else {
        promiseA.complete(Right(a))
      }
    })
  })

  suspend {
    suspendCoroutine { ca: Continuation<B> ->
      FxRunLoop.startCancelable(fb, connB, ctx) { either: Either<Throwable, B> ->
        either.fold({ error ->
          ca.resumeWith(Result.failure(error))
        }, { b ->
          ca.resumeWith(Result.success(b))
        })
      }
    }
  }.startCoroutine(asyncContinuation(ctx) { either ->
    either.fold({ error ->
      if (active.getAndSet(false)) { // if an error finishes first, stop the race.
        FxRunLoop.start(connA.cancel()) { r2 ->
          FxRunLoop.start(connC.cancel()) { r3 ->
            conn.pop()
            val errorResult = r2.fold(ifLeft = { e2 ->
              r3.fold(ifLeft = { e3 -> Platform.composeErrors(error, e2, e3) }, ifRight = { Platform.composeErrors(error, e2) })
            }, ifRight = {
              r3.fold(ifLeft = { e3 -> Platform.composeErrors(error, e3) }, ifRight = { error })
            })
            cb(Left(errorResult))
          }
        }
      } else {
        promiseB.complete(Left(error))
      }
    }, { b ->
      if (active.getAndSet(false)) {
        conn.pop()
        cb(Right(Right(Left(Tuple3(FxFiber(promiseA, connA), b, FxFiber(promiseC, connC))))))
      } else {
        promiseB.complete(Right(b))
      }
    })
  })

  suspend {
    suspendCoroutine { ca: Continuation<C> ->
      FxRunLoop.startCancelable(fc, connC, ctx) { either: Either<Throwable, C> ->
        either.fold({ error ->
          ca.resumeWith(Result.failure(error))
        }, { c ->
          ca.resumeWith(Result.success(c))
        })
      }
    }
  }.startCoroutine(asyncContinuation(ctx) { either ->
    either.fold({ error ->
      if (active.getAndSet(false)) { // if an error finishes first, stop the race.
        FxRunLoop.start(connA.cancel()) { r2 ->
          FxRunLoop.start(connB.cancel()) { r3 ->
            conn.pop()
            val errorResult = r2.fold(ifLeft = { e2 ->
              r3.fold(ifLeft = { e3 -> Platform.composeErrors(error, e2, e3) }, ifRight = { Platform.composeErrors(error, e2) })
            }, ifRight = {
              r3.fold(ifLeft = { e3 -> Platform.composeErrors(error, e3) }, ifRight = { error })
            })
            cb(Left(errorResult))
          }
        }
      } else {
        promiseC.complete(Left(error))
      }
    }, { c ->
      if (active.getAndSet(false)) {
        conn.pop()
        cb(Right(Right(Right(Tuple3(FxFiber(promiseA, connA), FxFiber(promiseB, connB), c)))))
      } else {
        promiseC.complete(Right(c))
      }
    })
  })
}
