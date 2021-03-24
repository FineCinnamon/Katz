package arrow.optics

import arrow.Kind
import arrow.KindDeprecation
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Tuple2
import arrow.core.Tuple3
import arrow.core.identity
import arrow.core.toT
import arrow.typeclasses.Applicative
import arrow.typeclasses.Functor
import arrow.typeclasses.Monoid

@Deprecated(KindDeprecation)
class ForPLens private constructor() {
  companion object
}
@Deprecated(KindDeprecation)
typealias PLensOf<S, T, A, B> = arrow.Kind4<ForPLens, S, T, A, B>
@Deprecated(KindDeprecation)
typealias PLensPartialOf<S, T, A> = arrow.Kind3<ForPLens, S, T, A>
@Deprecated(KindDeprecation)
typealias PLensKindedJ<S, T, A, B> = arrow.HkJ4<ForPLens, S, T, A, B>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
@Deprecated(KindDeprecation)
inline fun <S, T, A, B> PLensOf<S, T, A, B>.fix(): PLens<S, T, A, B> =
  this as PLens<S, T, A, B>

/**
 * [Lens] is a type alias for [PLens] which fixes the type arguments
 * and restricts the [PLens] to monomorphic updates.
 */
typealias Lens<S, A> = PLens<S, S, A, A>

typealias ForLens = ForPLens
typealias LensOf<S, A> = PLensOf<S, S, A, A>
typealias LensPartialOf<S> = Kind<ForLens, S>
typealias LensKindedJ<S, A> = PLensKindedJ<S, S, A, A>

/**
 * A [Lens] (or Functional Reference) is an optic that can focus into a structure for
 * getting, setting or modifying the focus (target).
 *
 * A (polymorphic) [PLens] is useful when setting or modifying a value for a constructed type
 * i.e. PLens<Tuple2<Double, Int>, Tuple2<String, Int>, Double, String>
 *
 * A [PLens] can be seen as a pair of functions:
 * - `get: (S) -> A` meaning we can focus into an `S` and extract an `A`
 * - `set: (B) -> (S) -> T` meaning we can focus into an `S` and set a value `B` for a target `A` and obtain a modified source `T`
 *
 * @param S the source of a [PLens]
 * @param T the modified source of a [PLens]
 * @param A the focus of a [PLens]
 * @param B the modified focus of a [PLens]
 */
interface PLens<S, T, A, B> : PLensOf<S, T, A, B> {

  fun get(s: S): A
  fun set(s: S, b: B): T

  companion object {

    fun <S> id() = PIso.id<S>().asLens()

    /**
     * [PLens] that takes either [S] or [S] and strips the choice of [S].
     */
    fun <S> codiagonal(): Lens<Either<S, S>, S> = Lens(
      get = { it.fold(::identity, ::identity) },
      set = { s, b -> s.bimap({ b }, { b }) }
    )

    /**
     * Invoke operator overload to create a [PLens] of type `S` with target `A`.
     * Can also be used to construct [Lens]
     */
    operator fun <S, T, A, B> invoke(get: (S) -> A, set: (S, B) -> T) = object : PLens<S, T, A, B> {
      override fun get(s: S): A = get(s)

      override fun set(s: S, b: B): T = set(s, b)
    }

    /**
     * [Lens] to operate on the head of a [NonEmptyList]
     */
    @JvmStatic
    fun <A> nonEmptyListHead(): Lens<NonEmptyList<A>, A> =
      Lens(
        get = NonEmptyList<A>::head,
        set = { nel, newHead -> NonEmptyList(newHead, nel.tail) }
      )

    /**
     * [Lens] to operate on the tail of a [NonEmptyList]
     */
    @JvmStatic
    fun <A> nonEmptyListTail(): Lens<NonEmptyList<A>, List<A>> =
      Lens(
        get = NonEmptyList<A>::tail,
        set = { nel, newTail -> NonEmptyList(nel.head, newTail) }
      )

    /**
     * [PLens] to focus into the first value of a [Pair]
     */
    @JvmStatic
    fun <A, B, R> pairPFirst(): PLens<Pair<A, B>, Pair<R, B>, A, R> =
      PLens(
        get = { it.first },
        set = { (_, b), r -> r to b }
      )

    /**
     * [Lens] to focus into the first value of a [Pair]
     */
    @JvmStatic
    fun <A, B> pairFirst(): Lens<Pair<A, B>, A> =
      pairPFirst()

    /**
     * [PLens] to focus into the second value of a [Pair]
     */
    @JvmStatic
    fun <A, B, R> pairPSecond(): PLens<Pair<A, B>, Pair<A, R>, B, R> =
      PLens(
        get = { it.second },
        set = { (a, _), r -> a to r }
      )

    /**
     * [Lens] to focus into the second value of a [Pair]
     */
    @JvmStatic
    fun <A, B> pairSecond(): Lens<Pair<A, B>, B> =
      pairPSecond()

    /**
     * [PLens] to focus into the first value of a [Triple]
     */
    @JvmStatic
    fun <A, B, C, R> triplePFirst(): PLens<Triple<A, B, C>, Triple<R, B, C>, A, R> =
      PLens(
        get = { it.first },
        set = { (_, b, c), r -> Triple(r, b, c) }
      )

    /**
     * [Lens] to focus into the first value of a [Triple]
     */
    @JvmStatic
    fun <A, B, C> tripleFirst(): Lens<Triple<A, B, C>, A> =
      triplePFirst()

    /**
     * [PLens] to focus into the second value of a [Triple]
     */
    @JvmStatic
    fun <A, B, C, R> triplePSecond(): PLens<Triple<A, B, C>, Triple<A, R, C>, B, R> =
      PLens(
        get = { it.second },
        set = { (a, _, c), r -> Triple(a, r, c) }
      )

    /**
     * [Lens] to focus into the second value of a [Triple]
     */
    @JvmStatic
    fun <A, B, C> tripleSecond(): Lens<Triple<A, B, C>, B> =
      triplePSecond()

    /**
     * [PLens] to focus into the third value of a [Triple]
     */
    @JvmStatic
    fun <A, B, C, R> triplePThird(): PLens<Triple<A, B, C>, Triple<A, B, R>, C, R> =
      PLens(
        get = { it.third },
        set = { (a, b, _), r -> Triple(a, b, r) }
      )

    /**
     * [Lens] to focus into the third value of a [Triple]
     */
    @JvmStatic
    fun <A, B, C> tripleThird(): Lens<Triple<A, B, C>, C> =
      triplePThird()

    /**
     * [PLens] to focus into the first value of a [arrow.core.Tuple2]
     */
    @JvmStatic
    fun <A, B, R> tuple2PFirst(): PLens<Tuple2<A, B>, Tuple2<R, B>, A, R> =
      PLens(
        get = { it.a },
        set = { (_, b), r -> r toT b }
      )

    /**
     * [Lens] to focus into the first value of a [arrow.core.Tuple2]
     */
    @JvmStatic
    fun <A, B> tuple2First(): Lens<Tuple2<A, B>, A> =
      tuple2PFirst()

    /**
     * [PLens] to focus into the second value of a [arrow.core.Tuple2]
     */
    @JvmStatic
    fun <A, B, R> tuple2PSecond(): PLens<Tuple2<A, B>, Tuple2<A, R>, B, R> =
      PLens(
        get = { it.b },
        set = { (a, _), r -> a toT r }
      )

    /**
     * [Lens] to focus into the second value of a [arrow.core.Tuple2]
     */
    @JvmStatic
    fun <A, B> tuple2Second(): Lens<Tuple2<A, B>, B> =
      tuple2PSecond()

    /**
     * [PLens] to focus into the first value of a [arrow.core.Tuple3]
     */
    @JvmStatic
    fun <A, B, C, R> tuple3PFirst(): PLens<Tuple3<A, B, C>, Tuple3<R, B, C>, A, R> =
      PLens(
        get = { it.a },
        set = { (_, b, c), r -> Tuple3(r, b, c) }
      )

    /**
     * [Lens] to focus into the first value of a [arrow.core.Tuple3]
     */
    @JvmStatic
    fun <A, B, C> tuple3First(): Lens<Tuple3<A, B, C>, A> =
      tuple3PFirst()

    /**
     * [PLens] to focus into the second value of a [arrow.core.Tuple3]
     */
    @JvmStatic
    fun <A, B, C, R> tuple3PSecond(): PLens<Tuple3<A, B, C>, Tuple3<A, R, C>, B, R> =
      PLens(
        get = { it.b },
        set = { (a, _, c), r -> Tuple3(a, r, c) }
      )

    /**
     * [Lens] to focus into the second value of a [arrow.core.Tuple3]
     */
    @JvmStatic
    fun <A, B, C> tuple3Second(): Lens<Tuple3<A, B, C>, B> =
      tuple3PSecond()

    /**
     * [PLens] to focus into the third value of a [arrow.core.Tuple3]
     */
    @JvmStatic
    fun <A, B, C, R> tuple3PThird(): PLens<Tuple3<A, B, C>, Tuple3<A, B, R>, C, R> =
      PLens(
        get = { it.c },
        set = { (a, b, _), r -> Tuple3(a, b, r) }
      )

    /**
     * [Lens] to focus into the third value of a [arrow.core.Tuple3]
     */
    @JvmStatic
    fun <A, B, C> tuple3Third(): Lens<Tuple3<A, B, C>, C> =
      tuple3PThird()
  }

  /**
   * Modify the focus of a [PLens] using Functor function
   */
  fun <F> modifyF(FF: Functor<F>, s: S, f: (A) -> Kind<F, B>): Kind<F, T> = FF.run {
    f(get(s)).map { b -> set(s, b) }
  }

  /**
   * Lift a function [f]: `(A) -> Kind<F, B> to the context of `S`: `(S) -> Kind<F, T>`
   */
  fun <F> liftF(FF: Functor<F>, f: (A) -> Kind<F, B>): (S) -> Kind<F, T> = { s -> modifyF(FF, s, f) }

  /**
   * Join two [PLens] with the same focus in [A]
   */
  infix fun <S1, T1> choice(other: PLens<S1, T1, A, B>): PLens<Either<S, S1>, Either<T, T1>, A, B> = PLens(
    { ss -> ss.fold(this::get, other::get) },
    { ss, b -> ss.bimap({ s -> set(s, b) }, { s -> other.set(s, b) }) }
  )

  /**
   * Pair two disjoint [PLens]
   */
  infix fun <S1, T1, A1, B1> split(other: PLens<S1, T1, A1, B1>): PLens<Tuple2<S, S1>, Tuple2<T, T1>, Tuple2<A, A1>, Tuple2<B, B1>> =
    PLens(
      { (s, c) -> get(s) toT other.get(c) },
      { (s, s1), (b, b1) -> set(s, b) toT other.set(s1, b1) }
    )

  /**
   * Create a product of the [PLens] and a type [C]
   */
  fun <C> first(): PLens<Tuple2<S, C>, Tuple2<T, C>, Tuple2<A, C>, Tuple2<B, C>> = PLens(
    { (s, c) -> get(s) toT c },
    { (s, _), (b, c) -> set(s, b) toT c }
  )

  /**
   * Create a product of a type [C] and the [PLens]
   */
  fun <C> second(): PLens<Tuple2<C, S>, Tuple2<C, T>, Tuple2<C, A>, Tuple2<C, B>> = PLens(
    { (c, s) -> c toT get(s) },
    { (_, s), (c, b) -> c toT set(s, b) }
  )

  /**
   * Compose a [PLens] with another [PLens]
   */
  infix fun <C, D> compose(l: PLens<A, B, C, D>): PLens<S, T, C, D> = Lens(
    { a -> l.get(get(a)) },
    { s, c -> set(s, l.set(get(s), c)) }
  )

  /**
   * Compose a [PLens] with a [POptional]
   */
  infix fun <C, D> compose(other: POptional<A, B, C, D>): POptional<S, T, C, D> = asOptional() compose other

  /**
   * Compose an [PLens] with a [PIso]
   */
  infix fun <C, D> compose(other: PIso<A, B, C, D>): PLens<S, T, C, D> = compose(other.asLens())

  /**
   * Compose an [PLens] with a [Getter]
   */
  infix fun <C> compose(other: Getter<A, C>): Getter<S, C> = asGetter() compose other

  /**
   * Compose an [PLens] with a [PSetter]
   */
  infix fun <C, D> compose(other: PSetter<A, B, C, D>): PSetter<S, T, C, D> = asSetter() compose other

  /**
   * Compose an [PLens] with a [PPrism]
   */
  infix fun <C, D> compose(other: PPrism<A, B, C, D>): POptional<S, T, C, D> = asOptional() compose other

  /**
   * Compose an [PLens] with a [Fold]
   */
  infix fun <C> compose(other: Fold<A, C>): Fold<S, C> = asFold() compose other

  /**
   * Compose an [PLens] with a [PTraversal]
   */
  infix fun <C, D> compose(other: PTraversal<A, B, C, D>): PTraversal<S, T, C, D> = asTraversal() compose other

  /**
   * Plus operator overload to compose lenses
   */
  operator fun <C, D> plus(other: PLens<A, B, C, D>): PLens<S, T, C, D> = compose(other)

  operator fun <C, D> plus(other: POptional<A, B, C, D>): POptional<S, T, C, D> = compose(other)

  operator fun <C, D> plus(other: PIso<A, B, C, D>): PLens<S, T, C, D> = compose(other)

  operator fun <C> plus(other: Getter<A, C>): Getter<S, C> = compose(other)

  operator fun <C, D> plus(other: PSetter<A, B, C, D>): PSetter<S, T, C, D> = compose(other)

  operator fun <C, D> plus(other: PPrism<A, B, C, D>): POptional<S, T, C, D> = compose(other)

  operator fun <C> plus(other: Fold<A, C>): Fold<S, C> = compose(other)

  operator fun <C, D> plus(other: PTraversal<A, B, C, D>): PTraversal<S, T, C, D> = compose(other)

  /**
   * View [PLens] as a [Getter]
   */
  fun asGetter(): Getter<S, A> = Getter(this::get)

  /**
   * View a [PLens] as a [POptional]
   */
  fun asOptional(): POptional<S, T, A, B> = POptional(
    { s -> Either.Right(get(s)) },
    { s, b -> set(s, b) }
  )

  /**
   * View a [PLens] as a [PSetter]
   */
  fun asSetter(): PSetter<S, T, A, B> = PSetter { s, f -> modify(s, f) }

  /**
   * View a [PLens] as a [Fold]
   */
  fun asFold(): Fold<S, A> = object : Fold<S, A> {
    override fun <R> foldMap(M: Monoid<R>, s: S, f: (A) -> R): R = f(get(s))
  }

  /**
   * View a [PLens] as a [PTraversal]
   */
  fun asTraversal(): PTraversal<S, T, A, B> = object : PTraversal<S, T, A, B> {
    override fun <F> modifyF(FA: Applicative<F>, s: S, f: (A) -> Kind<F, B>): Kind<F, T> = FA.run {
      f(get(s)).map { b -> this@PLens.set(s, b) }
    }
  }

  /**
   * Modify the focus of s [PLens] using s function `(A) -> B`
   */
  fun modify(s: S, f: (A) -> B): T = set(s, f(get(s)))

  /**
   * Lift a function [f]: `(A) -> B to the context of `S`: `(S) -> T`
   */
  fun lift(f: (A) -> B): (S) -> T = { s -> modify(s, f) }

  /**
   * Find a focus that satisfies the predicate
   */
  fun find(s: S, p: (A) -> Boolean): Option<A> = get(s).let { a ->
    if (p(a)) Some(a) else None
  }

  /**
   * Verify if the focus of a [PLens] satisfies the predicate
   */
  fun exist(s: S, p: (A) -> Boolean): Boolean = p(get(s))
}
