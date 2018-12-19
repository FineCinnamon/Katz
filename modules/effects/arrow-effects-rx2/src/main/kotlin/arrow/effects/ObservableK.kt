package arrow.effects

import arrow.Kind
import arrow.core.*
import arrow.effects.CoroutineContextRx2Scheduler.asScheduler
import arrow.effects.typeclasses.*
import arrow.higherkind
import arrow.typeclasses.Applicative
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlin.coroutines.CoroutineContext

fun <A> Observable<A>.k(): ObservableK<A> = ObservableK(this)

fun <A> ObservableKOf<A>.value(): Observable<A> =
  fix().observable

@higherkind
data class ObservableK<A>(val observable: Observable<A>) : ObservableKOf<A>, ObservableKKindedJ<A> {
  fun <B> map(f: (A) -> B): ObservableK<B> =
    observable.map(f).k()

  fun <B> ap(fa: ObservableKOf<(A) -> B>): ObservableK<B> =
    flatMap { a -> fa.fix().map { ff -> ff(a) } }

  fun <B> flatMap(f: (A) -> ObservableKOf<B>): ObservableK<B> =
    observable.flatMap { f(it).value() }.k()

  fun <B> bracketCase(use: (A) -> ObservableKOf<B>, release: (A, ExitCase<Throwable>) -> ObservableKOf<Unit>): ObservableK<B> =
    flatMap { a ->
      use(a).value()
        .doOnError { release(a, ExitCase.Error(it)) }
        .doOnDispose { release(a, ExitCase.Cancelled) }
        .doOnComplete { release(a, ExitCase.Completed) }
        .k()
    }

  fun <B> concatMap(f: (A) -> ObservableKOf<B>): ObservableK<B> =
    observable.concatMap { f(it).value() }.k()

  fun <B> switchMap(f: (A) -> ObservableKOf<B>): ObservableK<B> =
    observable.switchMap { f(it).value() }.k()

  fun <B> foldLeft(b: B, f: (B, A) -> B): B = observable.reduce(b, f).blockingGet()

  fun <B> foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> {
    fun loop(fa_p: ObservableK<A>): Eval<B> = when {
      fa_p.observable.isEmpty.blockingGet() -> lb
      else -> f(fa_p.observable.blockingFirst(), Eval.defer { loop(fa_p.observable.skip(1).k()) })
    }

    return Eval.defer { loop(this) }
  }

  fun <G, B> traverse(GA: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, ObservableK<B>> =
    foldRight(Eval.always { GA.just(Observable.empty<B>().k()) }) { a, eval ->
      GA.run { f(a).map2Eval(eval) { Observable.concat(Observable.just<B>(it.a), it.b.observable).k() } }
    }.value()

  fun handleErrorWith(function: (Throwable) -> ObservableKOf<A>): ObservableK<A> =
    value().onErrorResumeNext { t: Throwable -> function(t).value() }.k()

  fun continueOn(ctx: CoroutineContext): ObservableK<A> =
    observable.observeOn(ctx.asScheduler()).k()

  fun runAsync(cb: (Either<Throwable, A>) -> ObservableKOf<Unit>): ObservableK<Unit> =
    observable.flatMap { cb(Right(it)).value() }.onErrorResumeNext { t: Throwable -> cb(Left(t)).value() }.k()

  fun runAsyncCancellable(cb: (Either<Throwable, A>) -> ObservableKOf<Unit>): ObservableK<Disposable> =
    Observable.fromCallable {
      val disposable: io.reactivex.disposables.Disposable = runAsync(cb).value().subscribe()
      val dispose: () -> Unit = { disposable.dispose() }
      dispose
    }.k()

  override fun equals(other: Any?): Boolean =
    when (other) {
      is ObservableK<*> -> this.observable == other.observable
      is Observable<*> -> this.observable == other
      else -> false
    }

  override fun hashCode(): Int = observable.hashCode()

  companion object {
    fun <A> just(a: A): ObservableK<A> =
      Observable.just(a).k()

    fun <A> raiseError(t: Throwable): ObservableK<A> =
      Observable.error<A>(t).k()

    operator fun <A> invoke(fa: () -> A): ObservableK<A> =
      defer { just(fa()) }

    fun <A> defer(fa: () -> ObservableKOf<A>): ObservableK<A> =
      Observable.defer { fa().value() }.k()

    /**
     * Creates a [ObservableK] that'll run [ObservableKProc].
     *
     * {: data-executable='true'}
     *
     * ```kotlin:ank
     * import arrow.core.Either
     * import arrow.core.right
     * import arrow.effects.ObservableK
     * import arrow.effects.ObservableKConnection
     * import arrow.effects.value
     *
     * class Resource {
     *   fun asyncRead(f: (String) -> Unit): Unit = f("Some value of a resource")
     *   fun close(): Unit = Unit
     * }
     *
     * fun main(args: Array<String>) {
     *   //sampleStart
     *   val result = ObservableK.async { conn: ObservableKConnection, cb: (Either<Throwable, String>) -> Unit ->
     *     val resource = Resource()
     *     conn.push(ObservableK { resource.close() })
     *     resource.asyncRead { value -> cb(value.right()) }
     *   }
     *   //sampleEnd
     *   result.value().subscribe(::println)
     * }
     * ```
     */
    fun <A> async(fa: ObservableKProc<A>): ObservableK<A> =
      Observable.create<A> { emitter ->
        val connection = ObservableKConnection()
        //On disposing of the upstream stream this will be called by `setCancellable` so check if upstream is already disposed or not because
        //on disposing the stream will already be in a terminated state at this point so calling onError, in a terminated state, will blow everything up.
        connection.push(ObservableK { if (!emitter.isDisposed) emitter.onError(ConnectionCancellationException) })
        emitter.setCancellable {
          connection.cancel().value().observeOn(Schedulers.computation()).subscribe({}, {})
        }

        fa(connection) { either: Either<Throwable, A> ->
          either.fold({
            emitter.onError(it)
          }, {
            emitter.onNext(it)
            emitter.onComplete()
          })
        }
      }.k()

    tailrec fun <A, B> tailRecM(a: A, f: (A) -> ObservableKOf<Either<A, B>>): ObservableK<B> {
      val either = f(a).value().blockingFirst()
      return when (either) {
        is Either.Left -> tailRecM(either.a, f)
        is Either.Right -> Observable.just(either.b).k()
      }
    }
  }
}

fun <A, G> ObservableKOf<Kind<G, A>>.sequence(GA: Applicative<G>): Kind<G, ObservableK<A>> =
  fix().traverse(GA, ::identity)
