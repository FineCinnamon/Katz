package kategory

interface ApplicativeError<F, E> : Applicative<F>, Typeclass {

    fun <A> raiseError(e: E): HK<F, A>

    fun <A> handleErrorWith(fa: HK<F, A>, f: (E) -> HK<F, A>): HK<F, A>

    fun <A> handleError(fa: HK<F, A>, f: (E) -> A): HK<F, A> =
            handleErrorWith(fa) { pure(f(it)) }

    fun <A> attempt(fa: HK<F, A>): HK<F, Either<E, A>> =
            handleErrorWith(map(fa) { Either.Right(it) }) {
                pure(Either.Left(it))
            }
}

fun <F, A> ApplicativeError<F, Throwable>.catch(f: () -> A): HK<F, A> =
        try {
            pure(f())
        }
        catch (e: Throwable) {
            raiseError(e)
        }

inline fun <reified F, reified E> applicativeError(): ApplicativeError<F, E> =
        instance(InstanceParametrizedType(Monad::class.java, listOf(F::class.java, E::class.java)))
