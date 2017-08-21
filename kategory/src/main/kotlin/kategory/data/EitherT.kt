package kategory

/**
 * [EitherT]`<F, A, B>` is a light wrapper on an `F<`[Either]`<A, B>>` with some
 * convenient methods for working with this nested structure.
 *
 * It may also be said that [EitherT] is a monad transformer for [Either].
 */
@higherkind data class EitherT<F, A, B>(val MF: Monad<F>, val value: HK<F, Either<A, B>>) : EitherTKind<F, A, B>, EitherTKindJ<F, A, B> {

    companion object {

        inline operator fun <reified F, A, B> invoke(value: HK<F, Either<A, B>>, MF: Monad<F> = monad<F>()): EitherT<F, A, B> = EitherT(MF, value)

        @JvmStatic inline fun <reified F, A, B> pure(b: B, MF: Monad<F> = monad<F>()): EitherT<F, A, B> = right(b, MF)

        @JvmStatic inline fun <reified F, A, B> right(b: B, MF: Monad<F> = monad<F>()): EitherT<F, A, B> = EitherT(MF, MF.pure(Either.Right(b)))

        @JvmStatic inline fun <reified F, A, B> left(a: A, MF: Monad<F> = monad<F>()): EitherT<F, A, B> = EitherT(MF, MF.pure(Either.Left(a)))

        @JvmStatic inline fun <reified F, A, B> fromEither(value: Either<A, B>, MF: Monad<F> = monad<F>()): EitherT<F, A, B> = EitherT(MF, MF.pure(value))

        inline fun <F, L> instances(MF: Monad<F>): EitherTInstances<F, L> = object : EitherTInstances<F, L> {
            override fun MF(): Monad<F> = MF
        }

        inline fun <reified F, L> functor(MF: Monad<F> = monad<F>()): Functor<EitherTKindPartial<F, L>> = instances(MF)

        inline fun <reified F, L> applicative(MF: Monad<F> = monad<F>()): Applicative<EitherTKindPartial<F, L>> = instances(MF)

        inline fun <reified F, L> monad(MF: Monad<F> = monad<F>()): Monad<EitherTKindPartial<F, L>> = instances(MF)

        inline fun <reified F, L> monadError(MF: Monad<F> = monad<F>()): MonadError<EitherTKindPartial<F, L>, L> = instances(MF)

        inline fun <reified F, A> traverse(FF: Traverse<F> = traverse<F>(), MF: Monad<F> = monad<F>()): Traverse<EitherTKindPartial<F, A>> = object : EitherTTraverse<F, A> {
            override fun FF(): Traverse<F> = FF

            override fun MF(): Monad<F> = MF
        }

        inline fun <reified F, A> foldable(FF: Traverse<F> = traverse<F>(), MF: Monad<F> = monad<F>()): Foldable<EitherTKindPartial<F, A>> = traverse(FF, MF)

        inline fun <reified F, L> semigroupK(MF: Monad<F> = monad<F>()): SemigroupK<EitherTKindPartial<F, L>> = object : EitherTSemigroupK<F, L> {
            override fun F(): Monad<F> = MF
        }
    }

    inline fun <C> fold(crossinline l: (A) -> C, crossinline r: (B) -> C): HK<F, C> = MF.map(value, { either -> either.fold(l, r) })

    inline fun <C> flatMap(crossinline f: (B) -> EitherT<F, A, C>): EitherT<F, A, C> = flatMapF({ it -> f(it).value })

    inline fun <C> flatMapF(crossinline f: (B) -> HK<F, Either<A, C>>): EitherT<F, A, C> = EitherT(MF, MF.flatMap(value, { either -> either.fold({ MF.pure(Either.Left(it)) }, { f(it) }) }))

    inline fun <C> cata(crossinline l: (A) -> C, crossinline r: (B) -> C): HK<F, C> = fold(l, r)

    fun <C> liftF(fa: HK<F, C>): EitherT<F, A, C> = EitherT(MF, MF.map(fa, { Either.Right(it) }))

    inline fun <C> semiflatMap(crossinline f: (B) -> HK<F, C>): EitherT<F, A, C> = flatMap({ liftF(f(it)) })

    inline fun <C> map(crossinline f: (B) -> C): EitherT<F, A, C> = EitherT(MF, MF.map(value, { it.map(f) }))

    inline fun exists(crossinline p: (B) -> Boolean): HK<F, Boolean> = MF.map(value, { it.exists(p) })

    inline fun <C, D> transform(crossinline f: (Either<A, B>) -> Either<C, D>): EitherT<F, C, D> = EitherT(MF, MF.map(value, { f(it) }))

    inline fun <C> subflatMap(crossinline f: (B) -> Either<A, C>): EitherT<F, A, C> = transform({ it.flatMap(f) })

    fun toOptionT(): OptionT<F, B> = OptionT(MF, MF.map(value, { it.toOption() }))

    fun <C> foldL(b: C, f: (C, B) -> C, FF: Foldable<F>): C = FF.compose(Either.foldable<A>()).foldLC(value, b, f)

    fun <C> foldR(lb: Eval<C>, f: (B, Eval<C>) -> Eval<C>, FF: Foldable<F>): Eval<C> = FF.compose(Either.foldable<A>()).foldRC(value, lb, f)

    fun <G, C> traverse(f: (B) -> HK<G, C>, GA: Applicative<G>, FF: Traverse<F>, MF: Monad<F>): HK<G, HK<EitherTKindPartial<F, A>, C>> {
        val fa = ComposedTraverse(FF, Either.traverse<A>(), Either.monad<A>()).traverseC(value, f, GA)
        return GA.map(fa, { EitherT(MF, MF.map(it.lower(), { it.ev() })) })
    }
}
