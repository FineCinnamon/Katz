package kategory

typealias WriterTKind<F, W, A> = HK3<WriterT.F, F, W, A>
typealias WriterF<F, W> = HK2<WriterT.F, F, W>

fun <F, A, B> WriterTKind<F, A, B>.ev(): WriterT<F, A, B> =
        this as WriterT<F, A, B>

data class WriterT<F, W, A>(val MF: Monad<F>, val value: HK<F, Tuple2<W, A>>) : WriterTKind<F, W, A> {
    class F private constructor()

    companion object {
        inline fun <reified F, reified W, A> pure(a: A, MM: Monoid<W> = monoid(), MF: Monad<F> = kategory.monad()) =
                WriterT(MF.pure(MM.empty() toT a), MF)

        inline fun <reified F, W, A> both(w: W, a: A, MF: Monad<F> = kategory.monad()) =
                WriterT(MF.pure(w toT a), MF)

        inline fun <reified F, W, A> fromTuple(z: Tuple2<W, A>, MF: Monad<F> = kategory.monad()) =
                WriterT(MF.pure(z), MF)

        inline operator fun <reified F, W, A> invoke(value: HK<F, Tuple2<W, A>>, MF: Monad<F> = kategory.monad()) =
                WriterT(MF, value)

        inline fun <reified F, reified W> instances(MM: Monad<F> = kategory.monad<F>(), SG: Monoid<W> = kategory.monoid<W>()): WriterTInstances<F, W> = object : WriterTInstances<F, W> {
            override fun MM(): Monad<F> = MM

            override fun SG(): Monoid<W> = SG
        }

        inline fun <reified F, reified W> functor(MM: Monad<F> = kategory.monad<F>(), SG: Monoid<W> = kategory.monoid<W>()): Functor<WriterF<F, W>> = instances(MM, SG)

        inline fun <reified F, reified W> applicative(MM: Monad<F> = kategory.monad<F>(), SG: Monoid<W> = kategory.monoid<W>()): Applicative<WriterF<F, W>> = instances(MM, SG)

        inline fun <reified F, reified W> monad(MM: Monad<F> = kategory.monad<F>(), SG: Monoid<W> = kategory.monoid<W>()): Monad<WriterF<F, W>> = instances(MM, SG)

        inline fun <reified F, reified W> semigroupK(SGK: SemigroupK<F>): SemigroupK<WriterF<F, W>> = object : SemigroupK<WriterF<F, W>> {
            override fun <A> combineK(x: HK<WriterF<F, W>, A>, y: HK<WriterF<F, W>, A>): WriterT<F, W, A> =
                    WriterT(SGK.combineK(x.ev().value, y.ev().value))
        }
    }

    fun tell(w: W, SG: Semigroup<W>): WriterT<F, W, A> =
            mapAcc { SG.combine(it, w) }

    fun content(): HK<F, A> =
            MF.map(value, { it.b })

    fun write(): HK<F, W> =
            MF.map(value, { it.a })

    fun reset(MM: Monoid<W>): WriterT<F, W, A> =
            mapAcc { MM.empty() }

    inline fun <B> map(crossinline f: (A) -> B): WriterT<F, W, B> =
            WriterT(MF, MF.map(value, { it.a toT f(it.b) }))

    inline fun <U> mapAcc(crossinline f: (W) -> U): WriterT<F, U, A> =
            transform { f(it.a) toT it.b }

    inline fun <C, U> bimap(crossinline g: (W) -> U, crossinline f: (A) -> C): WriterT<F, U, C> =
            transform { g(it.a) toT f(it.b) }

    fun swap(): WriterT<F, A, W> =
            transform { it.b toT it.a }

    inline fun <B> flatMap(crossinline f: (A) -> WriterT<F, W, B>, SG: Semigroup<W>): WriterT<F, W, B> =
            WriterT(MF, MF.flatMap(value, { value -> MF.map(f(value.b).value, { SG.combine(it.a, value.a) toT it.b }) }))

    inline fun <B, U> transform(crossinline f: (Tuple2<W, A>) -> Tuple2<U, B>): WriterT<F, U, B> =
            WriterT(MF, MF.flatMap(value, { MF.pure(f(it)) }))

    fun <B> liftF(fa: HK<F, B>): WriterT<F, W, B> =
            WriterT(MF, MF.map2(fa, value, { it.b.a toT it.a }))

    inline fun <C> semiflatMap(crossinline f: (A) -> HK<F, C>, SG: Semigroup<W>): WriterT<F, W, C> =
            flatMap({ liftF(f(it)) }, SG)

    inline fun <B> subflatMap(crossinline f: (A) -> Tuple2<W, B>): WriterT<F, W, B> =
            transform({ f(it.b) })
}
