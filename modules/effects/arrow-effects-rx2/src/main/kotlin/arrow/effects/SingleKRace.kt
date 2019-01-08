package arrow.effects

import arrow.core.*
import arrow.effects.typeclasses.Fiber
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.ReplaySubject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import arrow.effects.CoroutineContextRx2Scheduler.asScheduler

fun <A, B> SingleK.Companion.racePair(ctx: CoroutineContext, fa: SingleKOf<A>, fb: SingleKOf<B>): SingleK<Either<Tuple2<A, Fiber<ForSingleK, B>>, Tuple2<Fiber<ForSingleK, A>, B>>> {
  val promiseA = ReplaySubject.create<A>().apply { toSerialized() }
  val promiseB = ReplaySubject.create<B>().apply { toSerialized() }
  val active = AtomicBoolean(true)
  val scheduler = ctx.asScheduler()

  val disposableA = CompositeDisposable()
  val cancelA = SingleK {
    disposableA.dispose()
    promiseA.onError(OnCancel.CancellationException)
  }
  val disposableB = CompositeDisposable()
  val cancelB = SingleK {
    disposableB.dispose()
    promiseB.onError(OnCancel.CancellationException)
  }

  return SingleK.async { conn, cb ->
    conn.pushPair(cancelA, cancelB)

    disposableA.add(fa.value()
      .observeOn(scheduler)
      .subscribeOn(scheduler)
      .subscribe({ a ->
        if (active.getAndSet(false)) {
          conn.pop()
          cb(Right(Left(Tuple2(a, Fiber(promiseB.firstOrError().k(), cancelB)))))
        } else {
          promiseA.onNext(a)
          promiseA.onComplete()
        }
      }, { error ->
        if (active.getAndSet(false)) { //if an error finishes first, stop the race.
          conn.pop()
          disposableB.dispose()
          promiseB.onError(OnCancel.CancellationException)
          cb(Left(error))
        } else {
          promiseA.onError(error)
        }
      }))

    disposableB.add(fb.value()
      .observeOn(scheduler)
      .subscribeOn(scheduler)
      .subscribe({ b ->
        if (active.getAndSet(false)) {
          conn.pop()
          cb(Right(Right(Tuple2(Fiber(promiseA.firstOrError().k(), cancelA), b))))
        } else {
          promiseB.onNext(b)
          promiseB.onComplete()
        }
      }, { error ->
        if (active.getAndSet(false)) { //if an error finishes first, stop the race.
          conn.pop()
          disposableA.dispose()
          promiseA.onError(OnCancel.CancellationException)
          cb(Left(error))
        } else {
          promiseB.onError(error)
        }
      }))
  }
}