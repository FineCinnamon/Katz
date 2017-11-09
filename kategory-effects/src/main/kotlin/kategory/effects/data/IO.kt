package kategory.effects

import kategory.*
import kategory.effects.internal.*
import kategory.effects.internal.Platform.onceOnly
import kategory.effects.internal.Platform.unsafeResync

@higherkind
@deriving(
        Functor::class,
        Applicative::class,
        Monad::class,
        AsyncContext::class)
sealed class IO<out A> : IOKind<A> {

    companion object {

        fun <A> pure(a: A): IO<A> = Pure(a)

        fun <A> raiseError(e: Throwable): IO<A> = RaiseError(e)

        internal fun <A, B> mapDefault(t: IO<A>, f: (A) -> B): IO<B> = t.flatMap(f.andThen { Pure(it) })

        internal fun <A> attemptValue(): AndThen<A, IO<Either<Throwable, A>>> =
                AndThen({ a: A -> Pure(Right(a)) }, { e -> Pure(Left(e)) })

        operator fun <A> invoke(f: () -> A): IO<A> = suspend { Pure(f()) }

        fun <A> suspend(f: () -> IO<A>): IO<A> =
                Suspend(AndThen { _ ->
                    try {
                        f()
                    } catch (throwable: Throwable) {
                        raiseError(throwable)
                    }
                })

        fun <A> runAsync(k: Proc<A>): IO<A> =
                Async { ff: (Either<Throwable, A>) -> Unit ->
                    onceOnly(ff).let { callback: (Either<Throwable, A>) -> Unit ->
                        try {
                            k(callback)
                        } catch (throwable: Throwable) {
                            callback(Left(throwable))
                        }
                    }
                }

        val unit: IO<Unit> =
                pure(Unit)

        fun <A> eval(eval: Eval<A>): IO<A> =
                when (eval) {
                    is Eval.Now -> pure(eval.value)
                    else -> invoke { eval.value() }
                }

        fun <A, B> tailRecM(a: A, f: (A) -> IOKind<Either<A, B>>): IO<B> =
                f(a).ev().flatMap {
                    when (it) {
                        is Left -> tailRecM(it.a, f)
                        is Right -> IO.pure(it.b)
                    }
                }

        fun <A, B> merge(
                op1: () -> A,
                op2: () -> B): IO<Tuple2<A, B>> =
                applicative().tupled(
                        invoke(op1),
                        invoke(op2)
                ).ev()

        fun <A, B, C> merge(
                op1: () -> A,
                op2: () -> B,
                op3: () -> C): IO<Tuple3<A, B, C>> =
                applicative().tupled(
                        invoke(op1),
                        invoke(op2),
                        invoke(op3)
                ).ev()

        fun <A, B, C, D> merge(
                op1: () -> A,
                op2: () -> B,
                op3: () -> C,
                op4: () -> D): IO<Tuple4<A, B, C, D>> =
                applicative().tupled(
                        invoke(op1),
                        invoke(op2),
                        invoke(op3),
                        invoke(op4)
                ).ev()

        fun <A, B, C, D, E> merge(
                op1: () -> A,
                op2: () -> B,
                op3: () -> C,
                op4: () -> D,
                op5: () -> E): IO<Tuple5<A, B, C, D, E>> =
                applicative().tupled(
                        invoke(op1),
                        invoke(op2),
                        invoke(op3),
                        invoke(op4),
                        invoke(op5)
                ).ev()

        fun <A, B, C, D, E, F> merge(
                op1: () -> A,
                op2: () -> B,
                op3: () -> C,
                op4: () -> D,
                op5: () -> E,
                op6: () -> F): IO<Tuple6<A, B, C, D, E, F>> =
                applicative().tupled(
                        invoke(op1),
                        invoke(op2),
                        invoke(op3),
                        invoke(op4),
                        invoke(op5),
                        invoke(op6)
                ).ev()

        fun <A, B, C, D, E, F, G> merge(
                op1: () -> A,
                op2: () -> B,
                op3: () -> C,
                op4: () -> D,
                op5: () -> E,
                op6: () -> F,
                op7: () -> G): IO<Tuple7<A, B, C, D, E, F, G>> =
                applicative().tupled(
                        invoke(op1),
                        invoke(op2),
                        invoke(op3),
                        invoke(op4),
                        invoke(op5),
                        invoke(op6),
                        invoke(op7)
                ).ev()

        fun <A, B, C, D, E, F, G, H> merge(
                op1: () -> A,
                op2: () -> B,
                op3: () -> C,
                op4: () -> D,
                op5: () -> E,
                op6: () -> F,
                op7: () -> G,
                op8: () -> H): IO<Tuple8<A, B, C, D, E, F, G, H>> =
                applicative().tupled(
                        invoke(op1),
                        invoke(op2),
                        invoke(op3),
                        invoke(op4),
                        invoke(op5),
                        invoke(op6),
                        invoke(op7),
                        invoke(op8)
                ).ev()

        fun <A, B, C, D, E, F, G, H, I> merge(
                op1: () -> A,
                op2: () -> B,
                op3: () -> C,
                op4: () -> D,
                op5: () -> E,
                op6: () -> F,
                op7: () -> G,
                op8: () -> H,
                op9: () -> I): IO<Tuple9<A, B, C, D, E, F, G, H, I>> =
                applicative().tupled(
                        invoke(op1),
                        invoke(op2),
                        invoke(op3),
                        invoke(op4),
                        invoke(op5),
                        invoke(op6),
                        invoke(op7),
                        invoke(op8),
                        invoke(op9)
                ).ev()

        fun <A, B, C, D, E, F, G, H, I, J> merge(
                op1: () -> A,
                op2: () -> B,
                op3: () -> C,
                op4: () -> D,
                op5: () -> E,
                op6: () -> F,
                op7: () -> G,
                op8: () -> H,
                op9: () -> I,
                op10: () -> J): IO<Tuple10<A, B, C, D, E, F, G, H, I, J>> =
                applicative().tupled(
                        invoke(op1),
                        invoke(op2),
                        invoke(op3),
                        invoke(op4),
                        invoke(op5),
                        invoke(op6),
                        invoke(op7),
                        invoke(op8),
                        invoke(op9),
                        invoke(op10)
                ).ev()
    }

    abstract fun <B> map(f: (A) -> B): IO<B>

    fun <B> flatMap(f: (A) -> IOKind<B>): IO<B> =
            flatMapTotal(
                    AndThen {
                        try {
                            f(it).ev()
                        } catch (error: Throwable) {
                            RaiseError<B>(error)
                        }
                    })

    internal abstract fun <B> flatMapTotal(f: AndThen<A, IO<B>>): IO<B>

    abstract fun attempt(): IO<Either<Throwable, A>>

    fun runAsync(cb: (Either<Throwable, A>) -> IO<Unit>): IO<Unit> = IO { unsafeRunAsync(cb.andThen { it.unsafeRunAsync { } }) }

    fun unsafeRunAsync(cb: (Either<Throwable, A>) -> Unit): Unit = unsafeStep().unsafeRunAsyncTotal(cb)

    internal abstract fun unsafeRunAsyncTotal(cb: (Either<Throwable, A>) -> Unit)

    @Suppress("UNCHECKED_CAST")
    fun unsafeStep(): IO<A> {
        var current: IO<A> = this
        while (true) {
            current = when (current) {
                is Suspend -> {
                    current.cont(Unit)
                }
                is BindSuspend<*, A> -> {
                    val cont: IO<Any?> = current.cont(Unit)
                    val f: AndThen<Any?, IO<A>> = current.f as AndThen<Any?, IO<A>>
                    cont.flatMapTotal(f)
                }
                else -> return current
            }
        }
    }

    fun unsafeRunSync(): A =
            unsafeRunTimed(Duration.INFINITE).fold({ throw IllegalArgumentException("IO execution should yield a valid result") }, { it })

    fun unsafeRunTimed(limit: Duration): Option<A> = unsafeStep().unsafeRunTimedTotal(limit)

    internal abstract fun unsafeRunTimedTotal(limit: Duration): Option<A>
}

internal data class Pure<out A>(val a: A) : IO<A>() {
    override fun <B> flatMapTotal(f: AndThen<A, IO<B>>): IO<B> = Suspend(AndThen({ _: Unit -> a }.andThen(f)))

    override fun <B> map(f: (A) -> B): IO<B> =
            try {
                Pure(f(a))
            } catch (exception: Throwable) {
                RaiseError(exception)
            }

    override fun attempt(): IO<Either<Throwable, A>> = Pure(Right(a))

    override fun unsafeRunAsyncTotal(cb: (Either<Throwable, A>) -> Unit) = cb(Right(a))

    override fun unsafeRunTimedTotal(limit: Duration): Option<A> = Some(a)
}

internal data class RaiseError<out A>(val exception: Throwable) : IO<A>() {
    override fun <B> map(f: (A) -> B): IO<B> = RaiseError(exception)

    override fun <B> flatMapTotal(f: AndThen<A, IO<B>>): IO<B> = Suspend(AndThen { f.error(exception, { RaiseError(it) }) })

    override fun attempt(): IO<Either<Throwable, A>> = Pure(Left(exception))

    override fun unsafeRunAsyncTotal(cb: (Either<Throwable, A>) -> Unit) = cb(Left(exception))

    override fun unsafeRunTimedTotal(limit: Duration): Option<A> = throw exception
}

internal data class Suspend<out A>(val cont: AndThen<Unit, IO<A>>) : IO<A>() {
    override fun <B> map(f: (A) -> B): IO<B> = mapDefault(this, f)

    override fun <B> flatMapTotal(f: AndThen<A, IO<B>>): IO<B> = BindSuspend(cont, f)

    override fun attempt(): IO<Either<Throwable, A>> = BindSuspend(cont, attemptValue())

    override fun unsafeRunAsyncTotal(cb: (Either<Throwable, A>) -> Unit) = throw AssertionError("Unreachable")

    override fun unsafeRunTimedTotal(limit: Duration): Option<A> = throw AssertionError("Unreachable")
}

internal data class BindSuspend<E, out A>(val cont: AndThen<Unit, IO<E>>, val f: AndThen<E, IO<A>>) : IO<A>() {
    override fun <B> map(f: (A) -> B): IO<B> = mapDefault(this, f)

    override fun <B> flatMapTotal(ff: AndThen<A, IO<B>>): IO<B> =
            BindSuspend(cont, f.andThen(AndThen({ it.flatMapTotal(ff) }, { ff.error(it, { RaiseError(it) }) })))

    override fun attempt(): IO<Either<Throwable, A>> = BindSuspend(AndThen { _ -> this }, attemptValue())

    override fun unsafeRunAsyncTotal(cb: (Either<Throwable, A>) -> Unit) = throw AssertionError("Unreachable")

    override fun unsafeRunTimedTotal(limit: Duration): Option<A> = throw AssertionError("Unreachable")
}

internal data class Async<out A>(val cont: Proc<A>) : IO<A>() {
    override fun <B> map(f: (A) -> B): IO<B> = mapDefault(this, f)

    override fun <B> flatMapTotal(f: AndThen<A, IO<B>>): IO<B> = BindAsync(cont, f)

    override fun attempt(): IO<Either<Throwable, A>> = BindAsync(cont, attemptValue())

    override fun unsafeRunAsyncTotal(cb: (Either<Throwable, A>) -> Unit) = cont(cb)

    override fun unsafeRunTimedTotal(limit: Duration): Option<A> = unsafeResync(this, limit)
}

internal data class BindAsync<E, out A>(val cont: ((Either<Throwable, E>) -> Unit) -> Unit, val f: AndThen<E, IO<A>>) : IO<A>() {
    override fun <B> map(f: (A) -> B): IO<B> = mapDefault(this, f)

    override fun <B> flatMapTotal(ff: AndThen<A, IO<B>>): IO<B> =
            BindAsync(cont, f.andThen(AndThen({ it.flatMapTotal(ff) }, { ff.error(it, { RaiseError(it) }) })))

    override fun attempt(): IO<Either<Throwable, A>> = BindSuspend(AndThen { _ -> this }, attemptValue())

    override fun unsafeRunAsyncTotal(cb: (Either<Throwable, A>) -> Unit) =
            cont { result ->
                try {
                    when (result) {
                        is Right -> f(result.b).unsafeRunAsync(cb)
                        is Left -> f.error(result.a, { RaiseError(it) }).unsafeRunAsync(cb)
                    }
                } catch (throwable: Throwable) {
                    cb(Left(throwable))
                }
            }

    override fun unsafeRunTimedTotal(limit: Duration): Option<A> = unsafeResync(this, limit)
}

fun <A, B> IO<A>.ap(ff: IOKind<(A) -> B>): IO<B> =
        flatMap { a -> ff.ev().map({ it(a) }) }

fun <A> IO<A>.handleErrorWith(f: (Throwable) -> IOKind<A>): IO<A> =
        attempt().flatMap { it.ev().fold(f, { IO.pure(it) }).ev() }

fun <A> A.liftIO(): IO<A> = IO.pure(this)
