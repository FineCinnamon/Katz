package arrow.core

import arrow.core.Either.Right
import arrow.typeclasses.Monoid
import arrow.typeclasses.Semigroup
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

/**
 *
 *
 * If you have worked with Java at all in the past, it is very likely that you have come across a `NullPointerException` at some time (other languages will throw similarly named errors in such a case). Usually this happens because some method returns `null` when you weren't expecting it and, thus, isn't dealing with that possibility in your client code. A value of `null` is often abused to represent an absent optional value.
 * Kotlin tries to solve the problem by getting rid of `null` values altogether, and providing its own special syntax [Null-safety machinery based on `?`](https://kotlinlang.org/docs/reference/null-safety.html).
 *
 * Arrow models the absence of values through the `Option` datatype similar to how Scala, Haskell, and other FP languages handle optional values.
 *
 * `Option<A>` is a container for an optional value of type `A`. If the value of type `A` is present, the `Option<A>` is an instance of `Some<A>`, containing the present value of type `A`. If the value is absent, the `Option<A>` is the object `None`.
 *
 * ```kotlin:ank:playground
 * import arrow.core.Option
 * import arrow.core.Some
 * import arrow.core.none
 *
 * //sampleStart
 * val someValue: Option<String> = Some("I am wrapped in something")
 * val emptyValue: Option<String> = none()
 * //sampleEnd
 * fun main() {
 *  println("value = $someValue")
 *  println("emptyValue = $emptyValue")
 * }
 * ```
 *
 * Let's write a function that may or may not give us a string, thus returning `Option<String>`:
 *
 * ```kotlin:ank
 * import arrow.core.None
 * import arrow.core.Option
 * import arrow.core.Some
 *
 * //sampleStart
 * fun maybeItWillReturnSomething(flag: Boolean): Option<String> =
 *  if (flag) Some("Found value") else None
 * //sampleEnd
 * ```
 *
 * Using `getOrElse`, we can provide a default value `"No value"` when the optional argument `None` does not exist:
 *
 * ```kotlin:ank:playground
 * import arrow.core.None
 * import arrow.core.Option
 * import arrow.core.Some
 * import arrow.core.getOrElse
 *
 * fun maybeItWillReturnSomething(flag: Boolean): Option<String> =
 *  if (flag) Some("Found value") else None
 *
 * val value1 =
 * //sampleStart
 *  maybeItWillReturnSomething(true)
 *     .getOrElse { "No value" }
 * //sampleEnd
 * fun main() {
 *  println(value1)
 * }
 * ```
 *
 * ```kotlin:ank:playground
 * import arrow.core.None
 * import arrow.core.Option
 * import arrow.core.Some
 * import arrow.core.getOrElse
 *
 * fun maybeItWillReturnSomething(flag: Boolean): Option<String> =
 *  if (flag) Some("Found value") else None
 *
 * val value2 =
 * //sampleStart
 *  maybeItWillReturnSomething(false)
 *   .getOrElse { "No value" }
 * //sampleEnd
 * fun main() {
 *  println(value2)
 * }
 * ```
 *
 * Checking whether option has value:
 *
 * ```kotlin:ank:playground
 * import arrow.core.None
 * import arrow.core.Option
 * import arrow.core.Some
 *
 * fun maybeItWillReturnSomething(flag: Boolean): Option<String> =
 *  if (flag) Some("Found value") else None
 *
 *  //sampleStart
 * val valueSome = maybeItWillReturnSomething(true) is None
 * val valueNone = maybeItWillReturnSomething(false) is None
 * //sampleEnd
 * fun main() {
 *  println("valueSome = $valueSome")
 *  println("valueNone = $valueNone")
 * }
 * ```
 * Creating a `Option<T>` of a `T?`. Useful for working with values that can be nullable:
 *
 * ```kotlin:ank:playground
 * import arrow.core.Option
 *
 * //sampleStart
 * val myString: String? = "Nullable string"
 * val option: Option<String> = Option.fromNullable(myString)
 * //sampleEnd
 * fun main () {
 *  println("option = $option")
 * }
 * ```
 *
 * Option can also be used with when statements:
 *
 * ```kotlin:ank:playground
 * import arrow.core.None
 * import arrow.core.Option
 * import arrow.core.Some
 *
 * //sampleStart
 * val someValue: Option<Double> = Some(20.0)
 * val value = when(someValue) {
 *  is Some -> someValue.value
 *  is None -> 0.0
 * }
 * //sampleEnd
 * fun main () {
 *  println("value = $value")
 * }
 * ```
 *
 * ```kotlin:ank:playground
 * import arrow.core.None
 * import arrow.core.Option
 * import arrow.core.Some
 *
 * //sampleStart
 * val noValue: Option<Double> = None
 * val value = when(noValue) {
 *  is Some -> noValue.value
 *  is None -> 0.0
 * }
 * //sampleEnd
 * fun main () {
 *  println("value = $value")
 * }
 * ```
 *
 * An alternative for pattern matching is folding. This is possible because an option could be looked at as a collection or foldable structure with either one or zero elements.
 *
 * One of these operations is `map`. This operation allows us to map the inner value to a different type while preserving the option:
 *
 * ```kotlin:ank:playground
 * import arrow.core.None
 * import arrow.core.Option
 * import arrow.core.Some
 *
 * //sampleStart
 * val number: Option<Int> = Some(3)
 * val noNumber: Option<Int> = None
 * val mappedResult1 = number.map { it * 1.5 }
 * val mappedResult2 = noNumber.map { it * 1.5 }
 * //sampleEnd
 * fun main () {
 *  println("number = $number")
 *  println("noNumber = $noNumber")
 *  println("mappedResult1 = $mappedResult1")
 *  println("mappedResult2 = $mappedResult2")
 * }
 * ```
 * Another operation is `fold`. This operation will extract the value from the option, or provide a default if the value is `None`
 *
 * ```kotlin:ank:playground
 * import arrow.core.Option
 * import arrow.core.Some
 *
 * val fold =
 * //sampleStart
 *  Some(3).fold({ 1 }, { it * 3 })
 * //sampleEnd
 * fun main () {
 *  println(fold)
 * }
 * ```
 *
 * ```kotlin:ank:playground
 * import arrow.core.Option
 * import arrow.core.none
 *
 * val fold =
 * //sampleStart
 *  none<Int>().fold({ 1 }, { it * 3 })
 * //sampleEnd
 * fun main () {
 *  println(fold)
 * }
 * ```
 *
 * Arrow also adds syntax to all datatypes so you can easily lift them into the context of `Option` where needed.
 *
 * ```kotlin:ank:playground
 * import arrow.core.some
 *
 * //sampleStart
 *  val some = 1.some()
 *  val none = none<String>()
 * //sampleEnd
 * fun main () {
 *  println("some = $some")
 *  println("none = $none")
 * }
 * ```
 *
 * ```kotlin:ank:playground
 * import arrow.core.toOption
 *
 * //sampleStart
 * val nullString: String? = null
 * val valueFromNull = nullString.toOption()
 *
 * val helloString: String? = "Hello"
 * val valueFromStr = helloString.toOption()
 * //sampleEnd
 * fun main () {
 *  println("valueFromNull = $valueFromNull")
 *  println("valueFromStr = $valueFromStr")
 * }
 * ```
 *
 * You can easily convert between `A?` and `Option<A>` by using the `toOption()` extension or `Option.fromNullable` constructor.
 *
 * ```kotlin:ank:playground
 * import arrow.core.firstOrNone
 * import arrow.core.toOption
 *
 * //sampleStart
 * val foxMap = mapOf(1 to "The", 2 to "Quick", 3 to "Brown", 4 to "Fox")
 *
 * val empty = foxMap.entries.firstOrNull { it.key == 5 }?.value.let { it?.toCharArray() }.toOption()
 * val filled = Option.fromNullable(foxMap.entries.firstOrNull { it.key == 5 }?.value.let { it?.toCharArray() })
 *
 * //sampleEnd
 * fun main() {
 *  println("empty = $empty")
 *  println("filled = $filled")
 * }
 * ```
 *
 * ### Transforming the inner contents
 *
 * ```kotlin:ank:playground
 * import arrow.core.Some
 *
 * fun main() {
 * val value =
 *  //sampleStart
 *    Some(1).map { it + 1 }
 *  //sampleEnd
 *  println(value)
 * }
 * ```
 *
 * ### Computing over independent values
 *
 * ```kotlin:ank:playground
 * import arrow.core.Some
 *
 *  val value =
 * //sampleStart
 *  Some(1).zip(Some("Hello"), Some(20.0), ::Triple)
 * //sampleEnd
 * fun main() {
 *  println(value)
 * }
 * ```
 *
 * ### Computing over dependent values ignoring absence
 *
 * ```kotlin:ank:playground
 * import arrow.core.computations.option
 * import arrow.core.Some
 * import arrow.core.Option
 *
 * suspend fun value(): Option<Int> =
 * //sampleStart
 *  option {
 *    val a = Some(1).bind()
 *    val b = Some(1 + a).bind()
 *    val c = Some(1 + b).bind()
 *    a + b + c
 * }
 * //sampleEnd
 * suspend fun main() {
 *  println(value())
 * }
 * ```
 *
 * ```kotlin:ank:playground
 * import arrow.core.computations.option
 * import arrow.core.Some
 * import arrow.core.none
 * import arrow.core.Option
 *
 * suspend fun value(): Option<Int> =
 * //sampleStart
 *  option {
 *    val x = none<Int>().bind()
 *    val y = Some(1 + x).bind()
 *    val z = Some(1 + y).bind()
 *    x + y + z
 *  }
 * //sampleEnd
 * suspend fun main() {
 *  println(value())
 * }
 * ```
 *
 * ## Credits
 *
 * Contents partially adapted from [Scala Exercises Option Tutorial](https://www.scala-exercises.org/std_lib/options)
 * Originally based on the Scala Koans.
 */
public sealed class Option<out A> {

  public companion object {

    @JvmStatic
    public fun <A> fromNullable(a: A?): Option<A> = if (a != null) Some(a) else None

    @JvmStatic
    public operator fun <A> invoke(a: A): Option<A> = Some(a)

    @JvmStatic
    @JvmName("tryCatchOrSideEffect")
    @Deprecated(
      "Side-effecting recover which loses Throwable is no longer supported",
      ReplaceWith("Option.catch({ recover(it); None }, f)")
    )
    public inline fun <A> catch(recover: (Throwable) -> Unit, f: () -> A): Option<A> =
      try {
        Some(f())
      } catch (t: Throwable) {
        recover(t.nonFatalOrThrow())
        None
      }

    @JvmStatic
    @JvmName("tryCatchOrNone")
    /**
     * Ignores exceptions and returns None if one is thrown
     */
    public inline fun <A> catch(f: () -> A): Option<A> {
      val recover: (Throwable) -> Option<A> = { None }
      return catch(recover, f)
    }

    @JvmStatic
    @JvmName("tryCatch")
    public inline fun <A> catch(recover: (Throwable) -> Option<A>, f: () -> A): Option<A> =
      try {
        Some(f())
      } catch (t: Throwable) {
        recover(t.nonFatalOrThrow())
      }

    @JvmStatic
    public fun <A, B> lift(f: (A) -> B): (Option<A>) -> Option<B> =
      { it.map(f) }
  }

  public fun <B> zip(other: Option<B>): Option<Pair<A, B>> =
    zip(other, ::Pair)

  public inline fun <B, C> zip(
    b: Option<B>,
    map: (A, B) -> C
  ): Option<C> =
    zip(
      b,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit
    ) { b, c, _, _, _, _, _, _, _, _ -> map(b, c) }

  public inline fun <B, C, D> zip(
    b: Option<B>,
    c: Option<C>,
    map: (A, B, C) -> D
  ): Option<D> =
    zip(
      b,
      c,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit
    ) { b, c, d, _, _, _, _, _, _, _ -> map(b, c, d) }

  public inline fun <B, C, D, E> zip(
    b: Option<B>,
    c: Option<C>,
    d: Option<D>,
    map: (A, B, C, D) -> E
  ): Option<E> =
    zip(
      b,
      c,
      d,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit,
      Some.unit
    ) { a, b, c, d, _, _, _, _, _, _ -> map(a, b, c, d) }

  public inline fun <B, C, D, E, F> zip(
    b: Option<B>,
    c: Option<C>,
    d: Option<D>,
    e: Option<E>,
    map: (A, B, C, D, E) -> F
  ): Option<F> =
    zip(b, c, d, e, Some.unit, Some.unit, Some.unit, Some.unit, Some.unit) { a, b, c, d, e, f, _, _, _, _ ->
      map(
        a,
        b,
        c,
        d,
        e
      )
    }

  public inline fun <B, C, D, E, F, G> zip(
    b: Option<B>,
    c: Option<C>,
    d: Option<D>,
    e: Option<E>,
    f: Option<F>,
    map: (A, B, C, D, E, F) -> G
  ): Option<G> =
    zip(b, c, d, e, f, Some.unit, Some.unit, Some.unit, Some.unit) { a, b, c, d, e, f, _, _, _, _ ->
      map(
        a,
        b,
        c,
        d,
        e,
        f
      )
    }

  public inline fun <B, C, D, E, F, G, H, I> zip(
    b: Option<B>,
    c: Option<C>,
    d: Option<D>,
    e: Option<E>,
    f: Option<F>,
    g: Option<G>,
    map: (A, B, C, D, E, F, G) -> H
  ): Option<H> =
    zip(b, c, d, e, f, g, Some.unit, Some.unit, Some.unit) { a, b, c, d, e, f, g, _, _, _ -> map(a, b, c, d, e, f, g) }

  public inline fun <B, C, D, E, F, G, H, I> zip(
    b: Option<B>,
    c: Option<C>,
    d: Option<D>,
    e: Option<E>,
    f: Option<F>,
    g: Option<G>,
    h: Option<H>,
    map: (A, B, C, D, E, F, G, H) -> I
  ): Option<I> =
    zip(b, c, d, e, f, g, h, Some.unit, Some.unit) { a, b, c, d, e, f, g, h, _, _ -> map(a, b, c, d, e, f, g, h) }

  public inline fun <B, C, D, E, F, G, H, I, J> zip(
    b: Option<B>,
    c: Option<C>,
    d: Option<D>,
    e: Option<E>,
    f: Option<F>,
    g: Option<G>,
    h: Option<H>,
    i: Option<I>,
    map: (A, B, C, D, E, F, G, H, I) -> J
  ): Option<J> =
    zip(b, c, d, e, f, g, h, i, Some.unit) { a, b, c, d, e, f, g, h, i, _ -> map(a, b, c, d, e, f, g, h, i) }

  public inline fun <B, C, D, E, F, G, H, I, J, K> zip(
    b: Option<B>,
    c: Option<C>,
    d: Option<D>,
    e: Option<E>,
    f: Option<F>,
    g: Option<G>,
    h: Option<H>,
    i: Option<I>,
    j: Option<J>,
    map: (A, B, C, D, E, F, G, H, I, J) -> K
  ): Option<K> =
    if (this is Some && b is Some && c is Some && d is Some && e is Some && f is Some && g is Some && h is Some && i is Some && j is Some) {
      Some(map(this.value, b.value, c.value, d.value, e.value, f.value, g.value, h.value, i.value, j.value))
    } else {
      None
    }

  /**
   * Returns true if the option is [None], false otherwise.
   * @note Used only for performance instead of fold.
   */
  public abstract fun isEmpty(): Boolean

  public fun isNotEmpty(): Boolean = !isEmpty()

  /**
   * alias for [isDefined]
   */
  public fun nonEmpty(): Boolean = isDefined()

  /**
   * Returns true if the option is an instance of [Some], false otherwise.
   * @note Used only for performance instead of fold.
   */
  public fun isDefined(): Boolean = !isEmpty()

  public fun orNull(): A? = fold({ null }, ::identity)

  /**
   * Returns a [Some<$B>] containing the result of applying $f to this $option's
   * value if this $option is nonempty. Otherwise return $none.
   *
   * @note This is similar to `flatMap` except here,
   * $f does not need to wrap its result in an $option.
   *
   * @param f the function to apply
   * @see flatMap
   */
  public inline fun <B> map(f: (A) -> B): Option<B> =
    flatMap { a -> Some(f(a)) }

  public inline fun <R> fold(ifEmpty: () -> R, ifSome: (A) -> R): R = when (this) {
    is None -> ifEmpty()
    is Some<A> -> ifSome(value)
  }

  /**
   * Returns $none if the result of applying $f to this $option's value is null.
   * Otherwise returns the result.
   *
   * @note This is similar to `.flatMap { Option.fromNullable(null)) }`
   * and primarily for convenience.
   *
   * @param f the function to apply.
   * */
  public inline fun <B> mapNotNull(f: (A) -> B?): Option<B> =
    flatMap { a -> fromNullable(f(a)) }

  /**
   * Returns the result of applying $f to this $option's value if
   * this $option is nonempty.
   * Returns $none if this $option is empty.
   * Slightly different from `map` in that $f is expected to
   * return an $option (which could be $none).
   *
   * @param f the function to apply
   * @see map
   */
  public inline fun <B> flatMap(f: (A) -> Option<B>): Option<B> =
    when (this) {
      is None -> this
      is Some -> f(value)
    }

  /**
   * Align two options (`this` on the left and [b] on the right) as one Option of [Ior].
   */
  public infix fun <B> align(b: Option<B>): Option<Ior<A, B>> =
    when (this) {
      None -> when (b) {
        None -> None
        is Some -> Some(b.value.rightIor())
      }
      is Some -> when (b) {
        None -> Some(this.value.leftIor())
        is Some -> Some(Pair(this.value, b.value).bothIor())
      }
    }

  /**
   * Align two options (`this` on the left and [b] on the right) as one Option of [Ior], and then, if it's not [None], map it using [f].
   *
   * @note This function works like a regular `align` function, but is then mapped by the `map` function.
   */
  public inline fun <B, C> align(b: Option<B>, f: (Ior<A, B>) -> C): Option<C> =
    align(b).map(f)

  /**
   * Returns true if this option is empty '''or''' the predicate
   * $predicate returns true when applied to this $option's value.
   *
   * @param predicate the predicate to test
   */
  public inline fun all(predicate: (A) -> Boolean): Boolean =
    fold({ true }, predicate)

  @Deprecated(
    "ap is deprecated alongside the Apply typeclass, since it's a low-level operator specific for generically deriving Apply combinators.",
    ReplaceWith(
      "ff.fix().flatMap { this.fix().map(it) }",
      "arrow.core.fix"
    )
  )
  public fun <B> ap(ff: Option<(A) -> B>): Option<B> =
    ff.flatMap { this.map(it) }

  public inline fun <B> crosswalk(f: (A) -> Option<B>): Option<Option<B>> =
    when (this) {
      is None -> this
      is Some -> f(value).map { Some(it) }
    }

  public inline fun <K, V> crosswalkMap(f: (A) -> Map<K, V>): Map<K, Option<V>> =
    when (this) {
      is None -> emptyMap()
      is Some -> f(value).mapValues { Some(it.value) }
    }

  public inline fun <B> crosswalkNull(f: (A) -> B?): Option<B>? =
    when (this) {
      is None -> null
      is Some -> f(value)?.let { Some(it) }
    }

  /**
   * Returns this $option if it is nonempty '''and''' applying the predicate $p to
   * this $option's value returns true. Otherwise, return $none.
   *
   *  @param predicate the predicate used for testing.
   */
  public inline fun filter(predicate: (A) -> Boolean): Option<A> =
    flatMap { a -> if (predicate(a)) Some(a) else None }

  /**
   * Returns this $option if it is nonempty '''and''' applying the predicate $p to
   * this $option's value returns false. Otherwise, return $none.
   *
   * @param predicate the predicate used for testing.
   */
  public inline fun filterNot(predicate: (A) -> Boolean): Option<A> =
    flatMap { a -> if (!predicate(a)) Some(a) else None }

  /**
   * Returns true if this option is nonempty '''and''' the predicate
   * $p returns true when applied to this $option's value.
   * Otherwise, returns false.
   *
   * Example:
   * ```
   * Some(12).exists { it > 10 } // Result: true
   * Some(7).exists { it > 10 }  // Result: false
   *
   * val none: Option<Int> = None
   * none.exists { it > 10 }      // Result: false
   * ```
   *
   * @param predicate the predicate to test
   */
  public inline fun exists(predicate: (A) -> Boolean): Boolean = fold({ false }, predicate)

  /**
   * Returns the $option's value if this option is nonempty '''and''' the predicate
   * $p returns true when applied to this $option's value.
   * Otherwise, returns null.
   *
   * Example:
   * ```
   * Some(12).exists { it > 10 } // Result: 12
   * Some(7).exists { it > 10 }  // Result: null
   *
   * val none: Option<Int> = None
   * none.exists { it > 10 }      // Result: null
   * ```
   */
  public inline fun findOrNull(predicate: (A) -> Boolean): A? =
    when (this) {
      is Some -> if (predicate(value)) value else null
      is None -> null
    }

  public inline fun <B> foldMap(MB: Monoid<B>, f: (A) -> B): B = MB.run {
    foldLeft(empty()) { b, a -> b.combine(f(a)) }
  }

  public inline fun <B> foldLeft(initial: B, operation: (B, A) -> B): B =
    when (this) {
      is Some -> operation(initial, value)
      is None -> initial
    }

  @Deprecated(FoldRightDeprecation)
  public inline fun <B> foldRight(initial: Eval<B>, crossinline operation: (A, Eval<B>) -> Eval<B>): Eval<B> =
    when (this) {
      is Some -> Eval.defer { operation(value, initial) }
      is None -> initial
    }

  public fun <B> padZip(other: Option<B>): Option<Pair<A?, B?>> =
    align(other) { ior ->
      ior.fold(
        { it to null },
        { null to it },
        { a, b -> a to b }
      )
    }

  public inline fun <B, C> padZip(other: Option<B>, f: (A?, B?) -> C): Option<C> =
    align(other) { ior ->
      ior.fold(
        { f(it, null) },
        { f(null, it) },
        { a, b -> f(a, b) }
      )
    }

  public inline fun <B> reduceOrNull(initial: (A) -> B, operation: (acc: B, A) -> B): B? =
    when (this) {
      is None -> null
      is Some -> operation(initial(value), value)
    }

  public inline fun <B> reduceRightEvalOrNull(
    initial: (A) -> B,
    operation: (A, acc: Eval<B>) -> Eval<B>
  ): Eval<B?> =
    when (this) {
      is None -> Eval.now(null)
      is Some -> operation(value, Eval.now(initial(value)))
    }

  public fun replicate(n: Int): Option<List<A>> =
    if (n <= 0) Some(emptyList()) else map { a -> List(n) { a } }

  public inline fun <B> traverse(fa: (A) -> Iterable<B>): List<Option<B>> =
    fold({ emptyList() }, { a -> fa(a).map { Some(it) } })

  public inline fun <AA, B> traverseEither(fa: (A) -> Either<AA, B>): Either<AA, Option<B>> =
    when (this) {
      is Some -> fa(value).map { Some(it) }
      is None -> Right(this)
    }

  public inline fun <AA, B> traverseValidated(fa: (A) -> Validated<AA, B>): Validated<AA, Option<B>> =
    when (this) {
      is Some -> fa(value).map { Some(it) }
      is None -> Valid(this)
    }

  public inline fun <L> toEither(ifEmpty: () -> L): Either<L, A> =
    fold({ ifEmpty().left() }, { it.right() })

  public fun toList(): List<A> = fold(::emptyList) { listOf(it) }

  public fun void(): Option<Unit> =
    map { Unit }

  public fun <L> pairLeft(left: L): Option<Pair<L, A>> = this.map { left to it }

  public fun <R> pairRight(right: R): Option<Pair<A, R>> = this.map { it to right }

  public infix fun <X> and(value: Option<X>): Option<X> = if (isEmpty()) {
    None
  } else {
    value
  }

  override fun toString(): String = fold(
    { "Option.None" },
    { "Option.Some($it)" }
  )
}

public object None : Option<Nothing>() {
  override fun isEmpty(): Boolean = true

  override fun toString(): String = "Option.None"
}

public data class Some<out T>(val value: T) : Option<T>() {
  override fun isEmpty(): Boolean = false

  override fun toString(): String = "Option.Some($value)"

  public companion object {
    @PublishedApi
    internal val unit: Option<Unit> = Some(Unit)
  }
}

/**
 * Returns the option's value if the option is nonempty, otherwise
 * return the result of evaluating `default`.
 *
 * @param default the default expression.
 */
public inline fun <T> Option<T>.getOrElse(default: () -> T): T = fold({ default() }, ::identity)

/**
 * Returns this option's if the option is nonempty, otherwise
 * returns another option provided lazily by `default`.
 *
 * @param alternative the default option if this is empty.
 */
public inline fun <A> Option<A>.orElse(alternative: () -> Option<A>): Option<A> =
  if (isEmpty()) alternative() else this

public infix fun <T> Option<T>.or(value: Option<T>): Option<T> = if (isEmpty()) {
  value
} else {
  this
}

public fun <T> T?.toOption(): Option<T> = this?.let { Some(it) } ?: None

public inline fun <A> Boolean.maybe(f: () -> A): Option<A> =
  if (this) {
    Some(f())
  } else {
    None
  }

public fun <A> A.some(): Option<A> = Some(this)

public fun <A> none(): Option<A> = None

public fun <A> Iterable<Option<A>>.combineAll(MA: Monoid<A>): Option<A> =
  fold(Option(MA.empty())) { acc, a ->
    acc.combine(MA, a)
  }

public fun <A> Option<A>.combineAll(MA: Monoid<A>): A = MA.run {
  foldLeft(empty()) { acc, a -> acc.combine(a) }
}

public inline fun <A> Option<A>.ensure(error: () -> Unit, predicate: (A) -> Boolean): Option<A> =
  when (this) {
    is Some ->
      if (predicate(value)) this
      else {
        error()
        None
      }
    is None -> this
  }

/**
 * Returns an Option containing all elements that are instances of specified type parameter [B].
 */
public inline fun <reified B> Option<*>.filterIsInstance(): Option<B> =
  flatMap {
    when (it) {
      is B -> Some(it)
      else -> None
    }
  }

public inline fun <A> Option<A>.handleError(f: (Unit) -> A): Option<A> =
  handleErrorWith { Some(f(Unit)) }

public inline fun <A> Option<A>.handleErrorWith(f: (Unit) -> Option<A>): Option<A> =
  if (isEmpty()) f(Unit) else this

public fun <A> Option<Option<A>>.flatten(): Option<A> =
  flatMap(::identity)

public inline fun <A, B> Option<A>.redeem(fe: (Unit) -> B, fb: (A) -> B): Option<B> =
  map(fb).handleError(fe)

public inline fun <A, B> Option<A>.redeemWith(fe: (Unit) -> Option<B>, fb: (A) -> Option<B>): Option<B> =
  flatMap(fb).handleErrorWith(fe)

public fun <A> Option<A>.replicate(n: Int, MA: Monoid<A>): Option<A> = MA.run {
  if (n <= 0) Some(empty())
  else map { a -> List(n) { a }.fold(empty()) { acc, v -> acc + v } }
}

public fun <A> Option<Either<Unit, A>>.rethrow(): Option<A> =
  flatMap { it.fold({ None }, { a -> Some(a) }) }

public fun <A> Option<A>.salign(SA: Semigroup<A>, b: Option<A>): Option<A> =
  align(b) {
    it.fold(::identity, ::identity) { a, b ->
      SA.run { a.combine(b) }
    }
  }

/**
 * Separate the inner [Either] value into the [Either.Left] and [Either.Right].
 *
 * @receiver Option of Either
 * @return a tuple containing Option of [Either.Left] and another Option of its [Either.Right] value.
 */
public fun <A, B> Option<Either<A, B>>.separateEither(): Pair<Option<A>, Option<B>> {
  val asep = flatMap { gab -> gab.fold({ Some(it) }, { None }) }
  val bsep = flatMap { gab -> gab.fold({ None }, { Some(it) }) }
  return asep to bsep
}

/**
 * Separate the inner [Validated] value into the [Validated.Invalid] and [Validated.Valid].
 *
 * @receiver Option of Either
 * @return a tuple containing Option of [Validated.Invalid] and another Option of its [Validated.Valid] value.
 */
public fun <A, B> Option<Validated<A, B>>.separateValidated(): Pair<Option<A>, Option<B>> {
  val asep = flatMap { gab -> gab.fold({ Some(it) }, { None }) }
  val bsep = flatMap { gab -> gab.fold({ None }, { Some(it) }) }
  return asep to bsep
}

public fun <A> Option<Iterable<A>>.sequence(): List<Option<A>> =
  traverse(::identity)

public fun <A, B> Option<Either<A, B>>.sequenceEither(): Either<A, Option<B>> =
  traverseEither(::identity)

public fun <A, B> Option<Validated<A, B>>.sequenceValidated(): Validated<A, Option<B>> =
  traverseValidated(::identity)

public fun <A, B> Option<Ior<A, B>>.unalign(): Pair<Option<A>, Option<B>> =
  unalign(::identity)

public inline fun <A, B, C> Option<C>.unalign(f: (C) -> Ior<A, B>): Pair<Option<A>, Option<B>> =
  when (val option = this.map(f)) {
    is None -> None to None
    is Some -> when (val v = option.value) {
      is Ior.Left -> Some(v.value) to None
      is Ior.Right -> None to Some(v.value)
      is Ior.Both -> Some(v.leftValue) to Some(v.rightValue)
    }
  }

public fun <A> Option<Iterable<A>>.unite(MA: Monoid<A>): Option<A> =
  map { iterable ->
    iterable.fold(MA)
  }

public fun <A, B> Option<Either<A, B>>.uniteEither(): Option<B> =
  flatMap { either ->
    either.fold({ None }, { b -> Some(b) })
  }

public fun <A, B> Option<Validated<A, B>>.uniteValidated(): Option<B> =
  flatMap { validated ->
    validated.fold({ None }, { b -> Some(b) })
  }

public fun <A, B> Option<Pair<A, B>>.unzip(): Pair<Option<A>, Option<B>> =
  unzip(::identity)

public inline fun <A, B, C> Option<C>.unzip(f: (C) -> Pair<A, B>): Pair<Option<A>, Option<B>> =
  fold(
    { None to None },
    { f(it).let { pair -> Some(pair.first) to Some(pair.second) } }
  )

/**
 *  Given [A] is a sub type of [B], re-type this value from Option<A> to Option<B>
 *
 *  Option<A> -> Option<B>
 *
 *  ```kotlin:ank:playground
 *  import arrow.core.Option
 *  import arrow.core.some
 *  import arrow.core.widen
 *
 *  fun main(args: Array<String>) {
 *   val result: Option<CharSequence> =
 *   //sampleStart
 *   "Hello".some().map({ "$it World" }).widen()
 *   //sampleEnd
 *   println(result)
 *  }
 *  ```
 */
public fun <B, A : B> Option<A>.widen(): Option<B> =
  this

public fun <K, V> Option<Pair<K, V>>.toMap(): Map<K, V> = this.toList().toMap()

public fun <A> Option<A>.combine(SGA: Semigroup<A>, b: Option<A>): Option<A> =
  when (this) {
    is Some -> when (b) {
      is Some -> Some(SGA.run { value.combine(b.value) })
      None -> this
    }
    None -> b
  }

public operator fun <A : Comparable<A>> Option<A>.compareTo(other: Option<A>): Int = fold(
  { other.fold({ 0 }, { -1 }) },
  { a1 ->
    other.fold({ 1 }, { a2 -> a1.compareTo(a2) })
  }
)
