package katz

interface IdMonad : Monad<Id.F> {
    override fun <A, B> map(fa: HK<Id.F, A>, f: (A) -> B): HK<Id.F, B> =
            fa.ev().map(f)

    override fun <A> pure(a: A): Id<A> = Id(a)

    override fun <A, B> flatMap(fa: IdKind<A>, f: (A) -> IdKind<B>): Id<B> =
            fa.ev().flatMap { f(it).ev() }

    @Suppress("UNCHECKED_CAST")
    tailrec override fun <A, B> tailRecM(a: A, f: (A) -> IdKind<Either<A, B>>): Id<B> {
        val x = f(a).ev().value
        return when (x) {
            is Either.Left<A> -> tailRecM(x.a, f)
            is Either.Right<B> -> Id(x.b)
        }
    }
}
