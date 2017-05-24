package katz

import katz.Eval.Companion.always

/**
 * Data structures that can be folded to a summary value.
 *
 * Foldable<F> is implemented in terms of two basic methods:
 *
 *  - `foldLeft(fa, b)(f)` eagerly folds `fa` from left-to-right.
 *  - `foldRight(fa, b)(f)` lazily folds `fa` from right-to-left.
 *
 * Beyond these it provides many other useful methods related to folding over F<A> values.
 */
interface Foldable<F> : Typeclass {

    /**
     * Left associative fold on F using the provided function.
     */
    fun <A, B> foldL(fa: HK<F, A>, b: B, f: (B, A) -> B): B

    /**
     * Right associative lazy fold on F using the provided function.
     *
     * This method evaluates lb lazily (in some cases it will not be needed), and returns a lazy value. We are using
     * (A, Eval<B>) => Eval<B> to support laziness in a stack-safe way. Chained computation should be performed via
     * .map and .flatMap.
     *
     * For more detailed information about how this method works see the documentation for Eval<A>.
     */
    fun <A, B> foldR(fa: HK<F, A>, lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B>

    /**
     * The size of this Foldable.
     *
     * This can be overriden in structures that have more efficient size implementations.
     *
     * Note: will not terminate for infinite-sized collections.
     */
    fun <A> size(ml: Monoid<Long>, fa: HK<F, A>): Long = foldMap(ml, fa)({ _ -> 1L })

    /**
     * Fold implemented using the given Monoid<A> instance.
     */
    fun <A> fold(ma: Monoid<A>, fa: HK<F, A>): A =
            foldL(fa, ma.empty(), { acc, a -> ma.combine(acc, a) })

    /**
     * Alias for fold.
     */
    fun <A : Monoid<*>> combineAll(m: Monoid<A>, fa: HK<F, A>): A = fold(m, fa)

    /**
     * Fold implemented by mapping A values into B and then combining them using the given Monoid<B> instance.
     */
    fun <A, B> foldMap(mb: Monoid<B>, fa: HK<F, A>): (f: (A) -> B) -> B =
            { f: (A) -> B -> foldL(fa, mb.empty(), { b, a -> mb.combine(b, f(a)) }) }

    /**
     * Left associative monadic folding on F.
     *
     * The default implementation of this is based on foldL, and thus will always fold across the entire structure.
     * Certain structures are able to implement this in such a way that folds can be short-circuited (not traverse the
     * entirety of the structure), depending on the G result produced at a given step.
     */
    fun <G, A, B> foldM(MG: Monad<G>, fa: HK<F, A>, z: B, f: (B, A) -> HK<G, B>): HK<G, B> {
        return foldL(fa, MG.pure(z), { gb, a -> MG.flatMap(gb) { f(it, a) } })
    }

    /**
     * Monadic folding on F by mapping A values to G<B>, combining the B values using the given Monoid<B> instance.
     *
     * Similar to foldM, but using a Monoid<B>.
     */
    fun <G, A, B> foldMapM(MG: Monad<G>, bb: Monoid<B>, fa: HK<F, A>, f: (A) -> HK<G, B>) : HK<G, B> {
        return foldM(MG, fa, bb.empty(), { b, a -> MG.map(f(a)) { bb.combine(b, it) } })
    }

    /**
     * Traverse F<A> using Applicative<G>.
     *
     * A typed values will be mapped into G<B> by function f and combined using Applicative#map2.
     *
     * This method is primarily useful when G<_> represents an action or effect, and the specific A aspect of G<A> is
     * not otherwise needed.
     */
    fun <G, A, B> traverse_(ag: Applicative<G>, fa: HK<F, A>, f: (A) -> HK<G, B>): HK<G, Unit> =
        foldR(fa, always { ag.pure(Unit) }, { a, acc -> ag.map2Eval(f(a), acc) { Unit } }).value()

    /**
     * Sequence F<G<A>> using Applicative<G>.
     *
     * Similar to traverse except it operates on F<G<A>> values, so no additional functions are needed.
     */
    fun <G, A> sequence_(ag: Applicative<G>, fga: HK<F, HK<G, A>>): HK<G, Unit> =
            traverse_(ag, fga, { it })

    /**
     * Check whether at least one element satisfies the predicate.
     *
     * If there are no elements, the result is false.
     */
    fun <A> exists(fa: HK<F, A>): (p: (A) -> Boolean) -> Boolean =
            { p: (A) -> Boolean -> foldR(fa, Eval.False, { a, lb -> if (p(a)) Eval.True else lb }).value() }

    /**
     * Check whether all elements satisfy the predicate.
     *
     * If there are no elements, the result is true.
     */
    fun <A> forall(fa: HK<F, A>): (p: (A) -> Boolean) -> Boolean =
            { p: (A) -> Boolean -> foldR(fa, Eval.True, { a, lb -> if (p(a)) lb else Eval.False }).value() }

    /**
     * Returns true if there are no elements. Otherwise false.
     */
    fun <A> isEmpty(fa: HK<F, A>): Boolean = foldR(fa, Eval.True, { _, _ -> Eval.False }).value()

    fun <A> nonEmpty(fa: HK<F, A>): Boolean = !isEmpty(fa)

    companion object {
        fun <A, B> iterateRight(it: Iterator<A>, lb: Eval<B>): (f: (A, Eval<B>) -> Eval<B>) -> Eval<B> = {
            f: (A, Eval<B>) -> Eval<B> ->
            fun loop(): Eval<B> =
                    Eval.defer { if (it.hasNext()) f(it.next(), loop()) else lb }
            loop()
        }
    }
}
