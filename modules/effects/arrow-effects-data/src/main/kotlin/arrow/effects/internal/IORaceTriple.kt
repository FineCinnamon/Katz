package arrow.effects.internal

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.Tuple3
import arrow.effects.ForIO
import arrow.effects.IOConnection
import arrow.effects.FxFiber
import arrow.effects.IO
import arrow.effects.IOOf
import arrow.effects.typeclasses.RaceTriple
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

interface IORaceTriple {

  /**
   * Race two tasks concurrently within a new [IO].
   * Race results in a winner and the other, yet to finish task running in a [Fiber].
   *
   * ```kotlin:ank:playground
   * import arrow.effects.suspended.fx.ForIO
   * import arrow.effects.suspended.fx.IO
   * import arrow.effects.typeclasses.Fiber
   * import arrow.effects.typeclasses.fold
   * import kotlinx.coroutines.Dispatchers
   * import java.lang.RuntimeException
   *
   * fun main(args: Array<String>) {
   *   //sampleStart
   *   val result = IO.raceTriple<Int, String, Double>(Dispatchers.Default, IO.never, IO { "I won the race" }, IO.never).flatMap {
   *     it.fold(
   *       { IO.raiseError<String>(RuntimeException("IO.never cannot win")) },
   *       { (_: Fiber<ForIO, Int>, res: String, _: Fiber<ForIO, Double>) -> IO.just(res) },
   *       { IO.raiseError(RuntimeException("IO.never cannot win")) }
   *     )
   *   }
   *   //sampleEnd
   *   println(IO.unsafeRunBlocking(result))
   * }
   * ```
   *
   * @param ctx [CoroutineContext] to execute the source [IO] on.
   * @param fa task to participate in the race
   * @param fb task to participate in the race
   * @return [IO] of [RaceTriple] which exposes a fold method to fold over the racing results in a elegant way.
   */
  fun <A, B, C> raceTriple(ctx: CoroutineContext, fa: IOOf<A>, fb: IOOf<B>, fc: IOOf<C>): IO<RaceTriple<ForIO, A, B, C>> = IO.async { conn, cb ->
    val active = AtomicBoolean(true)

    val upstreamCancelToken = IO.defer { if (conn.isCanceled()) IO.unit else conn.cancel() }

    val connA = IOConnection()
    connA.push(upstreamCancelToken)
    val promiseA = UnsafePromise<A>()

    val connB = IOConnection()
    connB.push(upstreamCancelToken)
    val promiseB = UnsafePromise<B>()

    val connC = IOConnection()
    connC.push(upstreamCancelToken)
    val promiseC = UnsafePromise<C>()

    conn.push(connA.cancel(), connB.cancel(), connC.cancel())

    suspend {
      suspendCoroutine { ca: Continuation<A> ->
        IORunLoop.startCancelable(fa, connA, ctx) { either: Either<Throwable, A> ->
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
          IORunLoop.start(connB.cancel()) { r2 ->
            IORunLoop.start(connC.cancel()) { r3 ->
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
        IORunLoop.startCancelable(fb, connB, ctx) { either: Either<Throwable, B> ->
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
          IORunLoop.start(connA.cancel()) { r2 ->
            IORunLoop.start(connC.cancel()) { r3 ->
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
        IORunLoop.startCancelable(fc, connC, ctx) { either: Either<Throwable, C> ->
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
          IORunLoop.start(connA.cancel()) { r2 ->
            IORunLoop.start(connB.cancel()) { r3 ->
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
}
