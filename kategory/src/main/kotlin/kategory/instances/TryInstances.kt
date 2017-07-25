package kategory

interface TryInstances :
        Functor<Try.F>,
        Applicative<Try.F>,
        Monad<Try.F>,
        MonadError<Try.F, Throwable>,
        Foldable<Try.F>,
        Traverse<Try.F> {

    override fun <A, B> map(fa: TryKind<A>, f: (A) -> B): Try<B> = fa.ev().map(f)

    override fun <A> pure(a: A): Try<A> = Try.Success(a)

    override fun <A, B> flatMap(fa: TryKind<A>, f: (A) -> TryKind<B>): Try<B> = fa.ev().flatMap { f(it).ev() }

    override fun <A> raiseError(e: Throwable): Try<A> = Try.Failure(e)

    override fun <A> handleErrorWith(fa: TryKind<A>, f: (Throwable) -> TryKind<A>): Try<A> =
            fa.ev().recoverWith { f(it).ev() }

    override fun <A, B> tailRecM(a: A, f: (A) -> TryKind<Either<A, B>>): Try<B> =
            f(a).ev().fold({ Try.raiseError(it) }, { either -> either.fold({ tailRecM(it, f) }, { Try.Success(it) }) })

    override fun <G, A, B> traverse(fa: HK<Try.F, A>, f: (A) -> HK<G, B>, GA: Applicative<G>): HK<G, HK<Try.F, B>> =
            fa.ev().fold({ GA.pure(Try.raise(IllegalStateException())) }, { GA.map(f(it), { Try { it } }) })

    override fun <A, B> foldL(fa: HK<Try.F, A>, b: B, f: (B, A) -> B): B =
            fa.ev().fold({ b }, { f(b, it) })

    override fun <A, B> foldR(fa: HK<Try.F, A>, lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
            fa.ev().fold({ lb }, { f(it, lb) })
}