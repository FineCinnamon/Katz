package arrow.optics

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.Option
import arrow.core.Tuple10
import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.Tuple6
import arrow.core.Tuple7
import arrow.core.Tuple8
import arrow.core.Tuple9
import arrow.core.identity
import arrow.typeclasses.Monoid
import kotlin.jvm.JvmStatic

/**
 * A [Fold] is an optic that allows to focus into structure and get multiple results.
 *
 * [Fold] is a generalisation of an instance of [Foldable] and is implemented in terms of foldMap.
 *
 * @param S the source of a [Fold]
 * @param A the target of a [Fold]
 */
interface Fold<S, A> {

  /**
   * Map each target to a type R and use a Monoid to fold the results
   */
  fun <R> foldMap(M: Monoid<R>, source: S, map: (focus: A) -> R): R

  /**
   * Calculate the number of targets
   */
  fun size(source: S) =
    foldMap(Monoid.int(), source) { 1 }

  /**
   * Check if all targets satisfy the predicate
   */
  fun all(source: S, predicate: (focus: A) -> Boolean): Boolean =
    foldMap(Monoid.boolean(), source, predicate)

  /**
   * Returns `true` if at least one focus matches the given [predicate].
   */
  fun any(source: S, predicate: (focus: A) -> Boolean): Boolean =
    foldMap(Monoid.booleanOr(), source, predicate)

  /**
   * Check if there is no target
   */
  fun isEmpty(source: S): Boolean =
    foldMap(Monoid.boolean(), source) { false }

  /**
   * Check if there is at least one target
   */
  fun isNotEmpty(source: S): Boolean =
    !isEmpty(source)

  /**
   * Get the first target or null
   */
  fun firstOrNull(source: S): A? =
    foldMap(firstOptionMonoid<A>(), source, ::identity)

  /**
   * Get the last target or null
   */ // TODO FIX
  fun lastOrNull(source: S): A? =
    foldMap(lastOptionMonoid<A>(), source, ::identity)

  /**
   * Fold using the given [Monoid] instance.
   */
  fun fold(M: Monoid<A>, source: S): A =
    foldMap(M, source, ::identity)

  /**
   * Alias for fold.
   */
  fun combineAll(M: Monoid<A>, source: S): A =
    foldMap(M, source, ::identity)

  /**
   * Get all targets of the [Fold]
   */
  fun getAll(source: S): List<A> =
    foldMap(Monoid.list(), source, ::listOf)

  /**
   * Find the first element matching the predicate, if one exists.
   */
  fun findOrNull(source: S, predicate: (focus: A) -> Boolean): A? =
    foldMap(firstOptionMonoid<A>(), source) { b -> if (predicate(b)) b else null }

  /**
   * Check whether at least one element satisfies the predicate.
   *
   * If there are no elements, the result is false.
   */
  fun exists(source: S, predicate: (focus: A) -> Boolean): Boolean =
    findOrNull(source, predicate)?.let { true } ?: false

  /**
   * Join two [Fold] with the same target
   */
  infix fun <C> choice(other: Fold<C, A>): Fold<Either<S, C>, A> =
    object : Fold<Either<S, C>, A> {
      override fun <R> foldMap(M: Monoid<R>, source: Either<S, C>, map: (focus: A) -> R): R =
        source.fold({ ac -> this@Fold.foldMap(M, ac, map) }, { c -> other.foldMap(M, c, map) })
    }

  /**
   * Create a sum of the [Fold] and a type [C]
   */
  fun <C> left(): Fold<Either<S, C>, Either<A, C>> =
    object : Fold<Either<S, C>, Either<A, C>> {
      override fun <R> foldMap(M: Monoid<R>, source: Either<S, C>, map: (Either<A, C>) -> R): R =
        source.fold({ a1: S -> this@Fold.foldMap(M, a1) { b -> map(Either.Left(b)) } }, { c -> map(Either.Right(c)) })
    }

  /**
   * Create a sum of a type [C] and the [Fold]
   */
  fun <C> right(): Fold<Either<C, S>, Either<C, A>> =
    object : Fold<Either<C, S>, Either<C, A>> {
      override fun <R> foldMap(M: Monoid<R>, source: Either<C, S>, map: (Either<C, A>) -> R): R =
        source.fold({ c -> map(Either.Left(c)) }, { a1 -> this@Fold.foldMap(M, a1) { b -> map(Either.Right(b)) } })
    }

  /**
   * Compose a [Fold] with a [Fold]
   */
  infix fun <C> compose(other: Fold<A, C>): Fold<S, C> =
    object : Fold<S, C> {
      override fun <R> foldMap(M: Monoid<R>, source: S, map: (focus: C) -> R): R =
        this@Fold.foldMap(M, source) { c -> other.foldMap(M, c, map) }
    }

  operator fun <C> plus(other: Fold<A, C>): Fold<S, C> =
    this compose other

  companion object {

    fun <A> id(): Fold<A, A> =
      PIso.id()

    /**
     * [Fold] that takes either [S] or [S] and strips the choice of [S].
     */
    fun <S> codiagonal() = object : Fold<Either<S, S>, S> {
      override fun <R> foldMap(M: Monoid<R>, source: Either<S, S>, map: (S) -> R): R =
        source.fold(map, map)
    }

    /**
     * Creates a [Fold] based on a predicate of the source [S]
     */
    fun <S> select(p: (S) -> Boolean): Fold<S, S> = object : Fold<S, S> {
      override fun <R> foldMap(M: Monoid<R>, source: S, map: (S) -> R): R =
        if (p(source)) map(source) else M.empty()
    }

    /**
     * [Fold] that points to nothing
     */
    fun <A, B> void(): Fold<A, B> =
      POptional.void()

    /**
     * [Traversal] for [List] that focuses in each [A] of the source [List].
     */
    @JvmStatic
    fun <A> list(): Fold<List<A>, A> =
      Every.list()

    /**
     * [Traversal] for [Either] that has focus in each [Either.Right].
     *
     * @receiver [Traversal.Companion] to make it statically available.
     * @return [Traversal] with source [Either] and focus every [Either.Right] of the source.
     */
    @JvmStatic
    fun <L, R> either(): Fold<Either<L, R>, R> =
      Every.either()

    @JvmStatic
    fun <K, V> map(): Fold<Map<K, V>, V> =
      Every.map()

    /**
     * [Traversal] for [NonEmptyList] that has focus in each [A].
     *
     * @receiver [PTraversal.Companion] to make it statically available.
     * @return [Traversal] with source [NonEmptyList] and focus every [A] of the source.
     */
    @JvmStatic
    fun <A> nonEmptyList(): Fold<NonEmptyList<A>, A> =
      Every.nonEmptyList()

    /**
     * [Traversal] for [Option] that has focus in each [arrow.core.Some].
     *
     * @receiver [PTraversal.Companion] to make it statically available.
     * @return [Traversal] with source [Option] and focus in every [arrow.core.Some] of the source.
     */
    @JvmStatic
    fun <A> option(): Fold<Option<A>, A> =
      Every.option()

    @JvmStatic
    fun <A> sequence(): Fold<Sequence<A>, A> =
      Every.sequence()

    /**
     * [Traversal] for [String] that focuses in each [Char] of the source [String].
     *
     * @receiver [PTraversal.Companion] to make it statically available.
     * @return [Traversal] with source [String] and foci every [Char] in the source.
     */
    @JvmStatic
    fun string(): Fold<String, Char> =
      Every.string()

    /**
     * [Traversal] to focus into the first and second value of a [Pair]
     */
    @JvmStatic
    fun <A> pair(): Fold<Pair<A, A>, A> =
      Every.pair()

    /**
     * [Traversal] to focus into the first, second and third value of a [Triple]
     */
    @JvmStatic
    fun <A> triple(): Fold<Triple<A, A, A>, A> =
      Every.triple()

    /**
     * [Traversal] to focus into the first, second, third and fourth value of a [arrow.core.Tuple4]
     */
    @JvmStatic
    fun <A> tuple4(): Fold<Tuple4<A, A, A, A>, A> =
      Every.tuple4()

    /**
     * [PTraversal] to focus into the first, second, third, fourth and fifth value of a [arrow.core.Tuple5]
     */
    @JvmStatic
    fun <A> tuple5(): Fold<Tuple5<A, A, A, A, A>, A> =
      Every.tuple5()

    /**
     * [Traversal] to focus into the first, second, third, fourth, fifth and sixth value of a [arrow.core.Tuple6]
     */
    @JvmStatic
    fun <A> tuple6(): Fold<Tuple6<A, A, A, A, A, A>, A> =
      Every.tuple6()

    /**
     * [Traversal] to focus into the first, second, third, fourth, fifth, sixth and seventh value of a [arrow.core.Tuple7]
     */
    @JvmStatic
    fun <A> tuple7(): Fold<Tuple7<A, A, A, A, A, A, A>, A> =
      Every.tuple7()

    /**
     * [Traversal] to focus into the first, second, third, fourth, fifth, sixth, seventh and eight value of a [arrow.core.Tuple8]
     */
    @JvmStatic
    fun <A> tuple8(): Fold<Tuple8<A, A, A, A, A, A, A, A>, A> =
      Every.tuple8()

    /**
     * [Traversal] to focus into the first, second, third, fourth, fifth, sixth, seventh, eight and ninth value of a [arrow.core.Tuple9]
     */
    @JvmStatic
    fun <A> tuple9(): Fold<Tuple9<A, A, A, A, A, A, A, A, A>, A> =
      Every.tuple9()

    /**
     * [Traversal] to focus into the first, second, third, fourth, fifth, sixth, seventh, eight, ninth and tenth value of a [arrow.core.Tuple10]
     */
    @JvmStatic
    fun <A> tuple10(): Fold<Tuple10<A, A, A, A, A, A, A, A, A, A>, A> =
      Every.tuple10()
  }
}
