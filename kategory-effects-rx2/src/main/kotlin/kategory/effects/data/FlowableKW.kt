package kategory.effects

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import kategory.*

fun <A> Flowable<A>.k(): FlowableKW<A> = FlowableKW(this)

fun <A> FlowableKWKind<A>.value(): Flowable<A> = this.ev().flowable

@higherkind
@deriving(
        Functor::class,
        Applicative::class,
        Monad::class,
        Foldable::class,
        Traverse::class
)
data class FlowableKW<A>(val flowable: Flowable<A>) : FlowableKWKind<A>, FlowableKWKindedJ<A> {

    fun <B> map(f: (A) -> B): FlowableKW<B> =
            flowable.map(f).k()

    fun <B> ap(fa: FlowableKWKind<(A) -> B>): FlowableKW<B> =
            flatMap { a -> fa.ev().map { ff -> ff(a) } }

    fun <B> flatMap(f: (A) -> FlowableKWKind<B>): FlowableKW<B> =
            flowable.flatMap { f(it).ev().flowable }.k()

    fun <B> concatMap(f: (A) -> FlowableKWKind<B>): FlowableKW<B> =
            flowable.concatMap { f(it).ev().flowable }.k()

    fun <B> switchMap(f: (A) -> FlowableKWKind<B>): FlowableKW<B> =
            flowable.switchMap { f(it).ev().flowable }.k()

    fun <B> foldLeft(b: B, f: (B, A) -> B): B = flowable.reduce(b, f).blockingGet()

    fun <B> foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> {
        fun loop(fa_p: FlowableKW<A>): Eval<B> = when {
            fa_p.flowable.isEmpty.blockingGet() -> lb
            else -> f(fa_p.flowable.blockingFirst(), Eval.defer { loop(fa_p.flowable.skip(1).k()) })
        }

        return Eval.defer { loop(this) }
    }

    fun <G, B> traverse(f: (A) -> HK<G, B>, GA: Applicative<G>): HK<G, FlowableKW<B>> =
            foldRight(Eval.always { GA.pure(Flowable.empty<B>().k()) }) { a, eval ->
                GA.map2Eval(f(a), eval) { Flowable.concat(Flowable.just<B>(it.a), it.b.flowable).k() }
            }.value()

    companion object {
        fun <A> pure(a: A): FlowableKW<A> =
                Flowable.just(a).k()

        fun <A> raiseError(t: Throwable): FlowableKW<A> =
                Flowable.error<A>(t).k()

        fun <A> runAsync(fa: Proc<A>, mode: BackpressureStrategy = BackpressureStrategy.BUFFER): FlowableKW<A> =
                Flowable.create({ emitter: FlowableEmitter<A> ->
                    fa { either: Either<Throwable, A> ->
                        either.fold({
                            emitter.onError(it)
                        }, {
                            emitter.onNext(it)
                            emitter.onComplete()
                        })

                    }
                }, mode).k()

        tailrec fun <A, B> tailRecM(a: A, f: (A) -> FlowableKWKind<Either<A, B>>): FlowableKW<B> {
            val either = f(a).ev().value().blockingFirst()
            return when (either) {
                is Either.Left -> tailRecM(either.a, f)
                is Either.Right -> Flowable.just(either.b).k()
            }
        }

        fun monadFlat(): FlowableKWMonadInstance = FlowableKWMonadInstanceImplicits.instance()

        fun monadConcat(): FlowableKWMonadInstance = object : FlowableKWMonadInstance {
            override fun <A, B> flatMap(fa: FlowableKWKind<A>, f: (A) -> FlowableKWKind<B>): FlowableKW<B> =
                    fa.ev().concatMap { f(it).ev() }
        }

        fun monadSwitch(): FlowableKWMonadInstance = object : FlowableKWMonadInstance {
            override fun <A, B> flatMap(fa: FlowableKWKind<A>, f: (A) -> FlowableKWKind<B>): FlowableKW<B> =
                    fa.ev().switchMap { f(it).ev() }
        }

        fun monadErrorFlat(): FlowableKWMonadErrorInstance = FlowableKWMonadErrorInstanceImplicits.instance()

        fun monadErrorConcat(): FlowableKWMonadErrorInstance = object : FlowableKWMonadErrorInstance {
            override fun <A, B> flatMap(fa: FlowableKWKind<A>, f: (A) -> FlowableKWKind<B>): FlowableKW<B> =
                    fa.ev().concatMap { f(it).ev() }
        }

        fun monadErrorSwitch(): FlowableKWMonadErrorInstance = object : FlowableKWMonadErrorInstance {
            override fun <A, B> flatMap(fa: FlowableKWKind<A>, f: (A) -> FlowableKWKind<B>): FlowableKW<B> =
                    fa.ev().switchMap { f(it).ev() }
        }

        fun asyncContextBuffer(): FlowableKWAsyncContextInstance = FlowableKWAsyncContextInstanceImplicits.instance()

        fun asyncContextDrop(): FlowableKWAsyncContextInstance = object : FlowableKWAsyncContextInstance {
            override fun BS(): BackpressureStrategy = BackpressureStrategy.DROP
        }

        fun asyncContextError(): FlowableKWAsyncContextInstance = object : FlowableKWAsyncContextInstance {
            override fun BS(): BackpressureStrategy = BackpressureStrategy.ERROR
        }

        fun asyncContextLatest(): FlowableKWAsyncContextInstance = object : FlowableKWAsyncContextInstance {
            override fun BS(): BackpressureStrategy = BackpressureStrategy.LATEST
        }

        fun asyncContextMissing(): FlowableKWAsyncContextInstance = object : FlowableKWAsyncContextInstance {
            override fun BS(): BackpressureStrategy = BackpressureStrategy.MISSING
        }
    }
}

fun <A> FlowableKWKind<A>.handleErrorWith(function: (Throwable) -> FlowableKW<A>): FlowableKW<A> =
        this.ev().flowable.onErrorResumeNext { t: Throwable -> function(t).flowable }.k()