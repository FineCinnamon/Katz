package arrow.instances

import arrow.Kind
import arrow.core.*
import arrow.data.OptionT
import arrow.data.OptionTOf
import arrow.data.OptionTPartialOf
import arrow.data.fix
import arrow.instance
import arrow.typeclasses.*

@instance(OptionT::class)
interface OptionTFunctorInstance<F> : Functor<OptionTPartialOf<F>> {

    fun FF(): Functor<F>

    override fun <A, B> Kind<OptionTPartialOf<F>, A>.map(f: (A) -> B): OptionT<F, B> = fix().map(FF(), f)

}

@instance(OptionT::class)
interface OptionTApplicativeInstance<F> : OptionTFunctorInstance<F>, Applicative<OptionTPartialOf<F>> {

    override fun FF(): Monad<F>

    override fun <A> just(a: A): OptionT<F, A> = OptionT(FF().just(Option(a)))

    override fun <A, B> Kind<OptionTPartialOf<F>, A>.map(f: (A) -> B): OptionT<F, B> = fix().map(FF(), f)

    override fun <A, B> Kind<OptionTPartialOf<F>, A>.ap(ff: Kind<OptionTPartialOf<F>, (A) -> B>): OptionT<F, B> =
            fix().ap(FF(), ff)
}

@instance(OptionT::class)
interface OptionTMonadInstance<F> : OptionTApplicativeInstance<F>, Monad<OptionTPartialOf<F>> {

    override fun <A, B> Kind<OptionTPartialOf<F>, A>.map(f: (A) -> B): OptionT<F, B> = fix().map(FF(), f)

    override fun <A, B> Kind<OptionTPartialOf<F>, A>.flatMap(f: (A) -> Kind<OptionTPartialOf<F>, B>): OptionT<F, B> = fix().flatMap(FF(), { f(it).fix() })

    override fun <A, B> Kind<OptionTPartialOf<F>, A>.ap(ff: Kind<OptionTPartialOf<F>, (A) -> B>): OptionT<F, B> =
            fix().ap(FF(), ff)

    override fun <A, B> tailRecM(a: A, f: (A) -> OptionTOf<F, Either<A, B>>): OptionT<F, B> =
            OptionT.tailRecM(FF(), a, f)

}

fun <F, A, B> OptionT<F, A>.foldLeft(b: B, f: (B, A) -> B, FF: Foldable<F>): B = FF.compose(Option.foldable()).foldLC(value, b, f)

fun <F, A, B> OptionT<F, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>, FF: Foldable<F>): Eval<B> = FF.compose(Option.foldable()).run {
    value.foldRC(lb, f)
}

fun <F, G, A, B> OptionT<F, A>.traverse(f: (A) -> Kind<G, B>, GA: Applicative<G>, FF: Traverse<F>): Kind<G, OptionT<F, B>> {
    val fa = ComposedTraverse(FF, Option.traverse(), Option.applicative()).traverseC(value, f, GA)
    return GA.run { fa.map({ OptionT(FF.run { it.unnest().map({ it.fix() }) }) }) }
}

@instance(OptionT::class)
interface OptionTFoldableInstance<F> : Foldable<OptionTPartialOf<F>> {

    fun FFF(): Foldable<F>

    override fun <A, B> Kind<OptionTPartialOf<F>, A>.foldLeft(b: B, f: (B, A) -> B): B =
            fix().foldLeft(b, f, FFF())

    override fun <A, B> Kind<OptionTPartialOf<F>, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
            fix().foldRight(lb, f, FFF())

}

@instance(OptionT::class)
interface OptionTTraverseInstance<F> : OptionTFoldableInstance<F>, Traverse<OptionTPartialOf<F>> {

    override fun FFF(): Traverse<F>

    override fun <G, A, B> Kind<OptionTPartialOf<F>, A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, OptionT<F, B>> =
            fix().traverse(f, AP, FFF())

}

@instance(OptionT::class)
interface OptionTSemigroupKInstance<F> : SemigroupK<OptionTPartialOf<F>> {

    fun FF(): Monad<F>

    override fun <A> Kind<OptionTPartialOf<F>, A>.combineK(y: Kind<OptionTPartialOf<F>, A>): OptionT<F, A> = fix().orElse(FF(), { y.fix() })
}

@instance(OptionT::class)
interface OptionTMonoidKInstance<F> : MonoidK<OptionTPartialOf<F>>, OptionTSemigroupKInstance<F> {
    override fun <A> empty(): OptionT<F, A> = OptionT(FF().just(None))
}
