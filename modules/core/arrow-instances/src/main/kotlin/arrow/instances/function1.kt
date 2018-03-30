package arrow.instances

import arrow.Kind
import arrow.typeclasses.Applicative
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.core.*
import arrow.data.*
import arrow.instance

@instance(Function1::class)
interface Function1FunctorInstance<I> : Functor<Function1PartialOf<I>> {
    override fun <A, B> Kind<Function1PartialOf<I>, A>.map(f: (A) -> B): Function1<I, B> =
            fix().map(f)
}

@instance(Function1::class)
interface Function1ApplicativeInstance<I> : Function1FunctorInstance<I>, Applicative<Function1PartialOf<I>> {

    override fun <A> just(a: A): Function1<I, A> =
            Function1.just(a)

    override fun <A, B> Kind<Function1PartialOf<I>, A>.map(f: (A) -> B): Function1<I, B> =
            fix().map(f)

    override fun <A, B> Kind<Function1PartialOf<I>, A>.ap(ff: Kind<Function1PartialOf<I>, (A) -> B>): Function1<I, B> =
            fix().ap(ff)
}

@instance(Function1::class)
interface Function1MonadInstance<I> : Function1ApplicativeInstance<I>, Monad<Function1PartialOf<I>> {

    override fun <A, B> Kind<Function1PartialOf<I>, A>.map(f: (A) -> B): Function1<I, B> =
            fix().map(f)

    override fun <A, B> Kind<Function1PartialOf<I>, A>.ap(ff: Kind<Function1PartialOf<I>, (A) -> B>): Function1<I, B> =
            fix().ap(ff)

    override fun <A, B> Kind<Function1PartialOf<I>, A>.flatMap(f: (A) -> Kind<Function1PartialOf<I>, B>): Function1<I, B> =
            fix().flatMap(f)

    override fun <A, B> tailRecM(a: A, f: (A) -> Function1Of<I, Either<A, B>>): Function1<I, B> =
            Function1.tailRecM(a, f)
}

