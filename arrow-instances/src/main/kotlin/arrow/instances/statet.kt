package arrow.instances

import arrow.*
import arrow.core.*
import arrow.data.*
import arrow.typeclasses.*

@instance(StateT::class)
interface StateTFunctorInstance<F, S> : Functor<StateTKindPartial<F, S>> {

    fun FF(): Functor<F>

    override fun <A, B> map(fa: StateTKind<F, S, A>, f: (A) -> B): StateT<F, S, B> = fa.ev().map(f, FF())

}

@instance(StateT::class)
interface StateTApplicativeInstance<F, S> : StateTFunctorInstance<F, S>, Applicative<StateTKindPartial<F, S>> {

    override fun FF(): Monad<F>

    override fun <A, B> map(fa: StateTKind<F, S, A>, f: (A) -> B): StateT<F, S, B> = fa.ev().map(f, FF())

    override fun <A> pure(a: A): StateT<F, S, A> = StateT(FF().pure({ s: S -> FF().pure(Tuple2(s, a)) }))

    override fun <A, B> ap(fa: StateTKind<F, S, A>, ff: StateTKind<F, S, (A) -> B>): StateT<F, S, B> =
            fa.ev().ap(ff, FF())

    override fun <A, B> product(fa: StateTKind<F, S, A>, fb: StateTKind<F, S, B>): StateT<F, S, Tuple2<A, B>> =
            fa.ev().product(fb.ev(), FF())

}

@instance(StateT::class)
interface StateTMonadInstance<F, S> : StateTApplicativeInstance<F, S>, Monad<StateTKindPartial<F, S>> {

    override fun <A, B> flatMap(fa: StateTKind<F, S, A>, f: (A) -> StateTKind<F, S, B>): StateT<F, S, B> =
            fa.ev().flatMap(f, FF())

    override fun <A, B> tailRecM(a: A, f: (A) -> StateTKind<F, S, Either<A, B>>): StateT<F, S, B> =
            StateT.tailRecM(a, f, FF())

    override fun <A, B> ap(fa: StateTKind<F, S, A>, ff: StateTKind<F, S, (A) -> B>): StateT<F, S, B> =
            ff.ev().map2(fa.ev(), { f, a -> f(a) }, FF())

}

@instance(StateT::class)
interface StateTSemigroupKInstance<F, S> : SemigroupK<StateTKindPartial<F, S>> {

    fun FF(): Monad<F>

    fun SS(): SemigroupK<F>

    override fun <A> combineK(x: StateTKind<F, S, A>, y: StateTKind<F, S, A>): StateT<F, S, A> =
            x.ev().combineK(y, FF(), SS())

}

@instance(StateT::class)
interface StateTMonadErrorInstance<F, S, E> : StateTMonadInstance<F, S>, MonadError<StateTKindPartial<F, S>, E> {
    override fun FF(): MonadError<F, E>

    override fun <A> raiseError(e: E): HK<StateTKindPartial<F, S>, A> = StateT.lift(FF(), FF().raiseError(e))

    override fun <A> handleErrorWith(fa: HK<StateTKindPartial<F, S>, A>, f: (E) -> HK<StateTKindPartial<F, S>, A>): StateT<F, S, A> =
            StateT(FF().pure({ s -> FF().handleErrorWith(fa.runM(FF(), s), { e -> f(e).runM(FF(), s) }) }))
}

/**
 * Alias for[StateT.Companion.applicative]
 */
fun <S> StateApi.applicative(): Applicative<StateTKindPartial<IdHK, S>> = StateT.applicative<IdHK, S>(arrow.typeclasses.monad<IdHK>(), dummy = Unit)

/**
 * Alias for [StateT.Companion.functor]
 */
fun <S> StateApi.functor(): Functor<StateTKindPartial<IdHK, S>> = StateT.functor<IdHK, S>(arrow.typeclasses.functor<IdHK>(), dummy = Unit)

/**
 * Alias for [StateT.Companion.monad]
 */
fun <S> StateApi.monad(): Monad<StateTKindPartial<IdHK, S>> = StateT.monad<IdHK, S>(arrow.typeclasses.monad<IdHK>(), dummy = Unit)

