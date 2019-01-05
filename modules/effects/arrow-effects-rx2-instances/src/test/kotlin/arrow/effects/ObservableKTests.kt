package arrow.effects

import arrow.effects.observablek.async.async
import arrow.effects.observablek.foldable.foldable
import arrow.effects.observablek.functor.functor
import arrow.effects.observablek.monad.flatMap
import arrow.effects.observablek.monadThrow.bindingCatch
import arrow.effects.observablek.traverse.traverse
import arrow.effects.typeclasses.ExitCase
import arrow.test.UnitSpec
import arrow.test.laws.*
import arrow.typeclasses.Eq
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.Spec
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(KTestJUnitRunner::class)
class ObservableKTests : UnitSpec() {

  fun <T> EQ(): Eq<ObservableKOf<T>> = object : Eq<ObservableKOf<T>> {
    override fun ObservableKOf<T>.eqv(b: ObservableKOf<T>): Boolean =
      try {
        this.value().blockingFirst() == b.value().blockingFirst()
      } catch (throwable: Throwable) {
        val errA = try {
          this.value().blockingFirst()
          throw IllegalArgumentException()
        } catch (err: Throwable) {
          err
        }

        val errB = try {
          b.value().blockingFirst()
          throw IllegalStateException()
        } catch (err: Throwable) {
          err
        }

        errA == errB
      }
  }

  override fun interceptSpec(context: Spec, spec: () -> Unit) {
    println("ObservableK: Skipping sync laws for stack safety because they are not supported. See https://github.com/ReactiveX/RxJava/issues/6322")
    super.interceptSpec(context, spec)
  }

  init {
    testLaws(AsyncLaws.laws(ObservableK.async(), EQ(), EQ(), testStackSafety = false))
//     FIXME(paco) #691
//    testLaws(AsyncLaws.laws(ObservableK.async(), EQ(), EQ()))
//    testLaws(AsyncLaws.laws(ObservableK.async(), EQ(), EQ()))

    testLaws(
      FoldableLaws.laws(ObservableK.foldable(), { ObservableK.just(it) }, Eq.any()),
      TraverseLaws.laws(ObservableK.traverse(), ObservableK.functor(), { ObservableK.just(it) }, EQ())
    )

    "Multi-thread Observables finish correctly" {
      val value: Observable<Long> = bindingCatch {
        val a = Observable.timer(2, TimeUnit.SECONDS).k().bind()
        a
      }.value()

      val test: TestObserver<Long> = value.test()
      test.awaitDone(5, TimeUnit.SECONDS)
      test.assertTerminated().assertComplete().assertNoErrors().assertValue(0)
    }

    "Multi-thread Observables should run on their required threads" {
      val originalThread: Thread = Thread.currentThread()
      var threadRef: Thread? = null
      val value: Observable<Long> = bindingCatch {
        val a = Observable.timer(2, TimeUnit.SECONDS, Schedulers.newThread()).k().bind()
        threadRef = Thread.currentThread()
        val b = Observable.just(a).observeOn(Schedulers.io()).k().bind()
        b
      }.value()
      val test: TestObserver<Long> = value.test()
      val lastThread: Thread = test.awaitDone(5, TimeUnit.SECONDS).lastThread()
      val nextThread = (threadRef?.name ?: "")

      nextThread shouldNotBe originalThread.name
      lastThread.name shouldNotBe originalThread.name
      lastThread.name shouldNotBe nextThread
    }

    "Observable cancellation forces binding to cancel without completing too" {
      val value: Observable<Long> = bindingCatch {
        val a = Observable.timer(3, TimeUnit.SECONDS).k().bind()
        a
      }.value()
      val test: TestObserver<Long> = value.doOnSubscribe { subscription -> Observable.timer(1, TimeUnit.SECONDS).subscribe { subscription.dispose() } }.test()
      test.awaitTerminalEvent(5, TimeUnit.SECONDS)

      test.assertNotTerminated().assertNotComplete().assertNoErrors().assertNoValues()
    }

    "ObservableK bracket cancellation should release resource with cancel exit status" {
      lateinit var ec: ExitCase<Throwable>
      val countDownLatch = CountDownLatch(1)

      ObservableK.just(Unit)
        .bracketCase(
          use = { ObservableK.async<Nothing> { _, _ -> } },
          release = { _, exitCase ->
            ObservableK {
              ec = exitCase
              countDownLatch.countDown()
            }
          }
        )
        .value()
        .subscribe()
        .dispose()

      countDownLatch.await(100, TimeUnit.MILLISECONDS)
      ec shouldBe ExitCase.Canceled
    }

    "ObservableK should cancel KindConnection on dispose" {
      Promise.uncancelable<ForObservableK, Unit>(ObservableK.async()).flatMap { latch ->
        ObservableK {
          ObservableK.async<Unit> { conn, _ ->
            conn.push(latch.complete(Unit))
          }.observable.subscribe().dispose()
        }.flatMap { latch.get() }
      }.value()
        .test()
        .assertValue(Unit)
        .awaitTerminalEvent(100, TimeUnit.MILLISECONDS)
    }

    "ObservableK async should be cancellable" {
      Promise.uncancelable<ForObservableK, Unit>(ObservableK.async())
        .flatMap { latch ->
          ObservableK {
            ObservableK.async<Unit> { _, _ -> }
              .value()
              .doOnDispose { latch.complete(Unit).value().subscribe() }
              .subscribe()
              .dispose()
          }.flatMap { latch.get() }
        }.observable
        .test()
        .assertValue(Unit)
        .awaitTerminalEvent(100, TimeUnit.MILLISECONDS)
    }


    "ObservableK should cancel KindConnection on dipose" {
      Promise.uncancelable<ForObservableK, Unit>(ObservableK.async()).flatMap { latch ->
        ObservableK {
          ObservableK.async<Unit> { conn, _ ->
            conn.push(latch.complete(Unit))
          }.observable.subscribe().dispose()
        }.flatMap { latch.get() }
      }.value()
        .test()
        .assertValue(Unit)
        .awaitTerminalEvent(100, TimeUnit.MILLISECONDS)
    }

    "KindConnection can cancel upstream" {
      ObservableK.async<Unit> { connection, _ ->
        connection.cancel().value().subscribe()
      }.observable
        .test()
        .assertError { it is ConnectionCancellationException }
    }

  }
}
