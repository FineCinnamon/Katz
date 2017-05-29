package katz

typealias EitherTKind<F, A, B> = HK3<EitherT.F, F, A, B>
typealias EitherTF<F, L> = HK2<EitherT.F, F, L>

/**
 * [EitherT]`<F, A, B>` is a light wrapper on an `F<`[Either]`<A, B>>` with some
 * convenient methods for working with this nested structure.
 *
 * It may also be said that [EitherT] is a monad transformer for [Either].
 */
data class EitherT<F, A, B>(val MF: Monad<F>, val value: HK<F, Either<A, B>>) : EitherTKind<F, A, B> {

    class F private constructor()

    private val CFE: ComposedType<F, EitherF<A>> = ComposedType()

    companion object {

        inline operator fun <reified F, A, B> invoke(value: HK<F, Either<A, B>>, MF: Monad<F> = monad<F>()): EitherT<F, A, B> = EitherT(MF, value)

        @JvmStatic inline fun <reified F, A, B> pure(b: B, MF: Monad<F> = monad<F>()): EitherT<F, A, B> = right(b, MF)

        @JvmStatic inline fun <reified F, A, B> right(b: B, MF: Monad<F> = monad<F>()): EitherT<F, A, B> = EitherT(MF, MF.pure(Either.Right(b)))

        @JvmStatic inline fun <reified F, A, B> left(a: A, MF: Monad<F> = monad<F>()): EitherT<F, A, B> = EitherT(MF, MF.pure(Either.Left(a)))

        @JvmStatic inline fun <reified F, A, B> fromEither(value: Either<A, B>, MF: Monad<F> = monad<F>()): EitherT<F, A, B> = EitherT(MF, MF.pure(value))
    }

    inline fun <C> fold(crossinline l: (A) -> C, crossinline r: (B) -> C): HK<F, C> =
            MF.map(value, { either -> either.fold(l, r) })

    inline fun <C> flatMap(crossinline f: (B) -> EitherT<F, A, C>): EitherT<F, A, C> =
            flatMapF({ it -> f(it).value })

    inline fun <C> flatMapF(crossinline f: (B) -> HK<F, Either<A, C>>): EitherT<F, A, C> =
            EitherT(MF, MF.flatMap(value, { either -> either.fold({ MF.pure(Either.Left(it)) }, { f(it) }) }))

    inline fun <C> cata(crossinline l: (A) -> C, crossinline r: (B) -> C): HK<F, C> =
            fold(l, r)

    fun <C> liftF(fa: HK<F, C>): EitherT<F, A, C> =
            EitherT(MF, MF.map(fa, { Either.Right(it) }))

    inline fun <C> semiflatMap(crossinline f: (B) -> HK<F, C>): EitherT<F, A, C> =
            flatMap({ liftF(f(it)) })

    inline fun <C> map(crossinline f: (B) -> C): EitherT<F, A, C> =
            EitherT(MF, MF.map(value, { it.map(f) }))

    inline fun exists(crossinline p: (B) -> Boolean): HK<F, Boolean> =
            MF.map(value, { it.exists(p) })

    inline fun <C, D> transform(crossinline f: (Either<A, B>) -> Either<C, D>): EitherT<F, C, D> =
            EitherT(MF, MF.map(value, { f(it) }))

    inline fun <C> subflatMap(crossinline f: (B) -> Either<A, C>): EitherT<F, A, C> =
            transform({ it.flatMap(f) })

    fun toOptionT(): OptionT<F, B> =
            OptionT(MF, MF.map(value, { it.toOption() }))

    fun <C> foldL(b: C, f: (C, B) -> C, FF: Foldable<F>): C =
            FF.compose(EitherTraverse<A>()).foldL(CFE.unapply(value), b, f)

    fun <C> foldR(lb: Eval<C>, f: (B, Eval<C>) -> Eval<C>, FF: Foldable<F>): Eval<C> =
            FF.compose(EitherTraverse<A>()).foldR(CFE.unapply(value), lb, f)

    fun <G, C> traverse(f: (B) -> HK<G, C>, GA: Applicative<G>, FF: Traverse<F>, MF: Monad<F>): HK<G, HK<EitherTF<F, A>, C>> {
        val fa = ComposedTraverse(FF, EitherTraverse<A>(), EitherMonad<A>(), CFE).traverse(CFE.unapply(value), f, GA)
        return GA.map(fa, { EitherT(MF, MF.map(CFE.apply(it), { it.ev() })) })
    }
}
