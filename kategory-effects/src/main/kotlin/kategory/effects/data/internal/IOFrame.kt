package kategory.effects

import kategory.Either

interface IOFrame<in A, out R> : (A) -> R {
    override operator fun invoke(a: A): R

    fun recover(e: Throwable): R

    fun fold(value: Either<Throwable, A>): R =
            when (value) {
                is Either.Right -> invoke(value.b)
                is Either.Left -> recover(value.a)
            }

    companion object {
        fun <A> errorHandler(fe: (Throwable) -> IO<A>): IOFrame<A, IO<A>> =
                ErrorHandler(fe)


        data class ErrorHandler<A>(val fe: (Throwable) -> IO<A>) : IOFrame<A, IO<A>> {
            override fun invoke(a: A): IO<A> = Pure(a)

            override fun recover(e: Throwable): IO<A> = fe(e)
        }

        @Suppress("UNCHECKED_CAST")
        fun <A> any(): (A) -> IO<Either<Throwable, A>> = AttemptIO as (A) -> IO<Either<Throwable, A>>

        private object AttemptIO : IOFrame<Any, IO<Either<Throwable, Any>>> {
            override operator fun invoke(a: Any): IO<Either<Nothing, Any>> = Pure(Either.Right(a))

            override fun recover(e: Throwable): IO<Either<Throwable, Nothing>> = Pure(Either.Left(e))
        }
    }
}