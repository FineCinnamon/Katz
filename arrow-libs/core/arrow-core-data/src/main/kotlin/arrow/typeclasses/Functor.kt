package arrow.typeclasses

import arrow.Kind
import arrow.KindDeprecation
import arrow.core.Tuple2
import arrow.documented

@Deprecated(KindDeprecation)
/**
 * The [Functor] type class abstracts the ability to [map] over the computational context of a type constructor.
 * Examples of type constructors that can implement instances of the Functor type class include
 * [arrow.core.Option], [arrow.core.NonEmptyList], [List] and many other data types that include a [map] function with the shape
 * `fun <F, A, B> Kind<F, A>.map(f: (A) -> B): Kind<F, B>` where `F` refers to any type constructor whose contents can be transformed.
 *
 * ### Example
 *
 * Oftentimes we find ourselves in situations where we need to transform the contents of some data type.
 * [map] allows us to safely compute over values under the assumption that they'll be there returning the
 * transformation encapsulated in the same context.
 *
 * Consider [arrow.core.Option] and [arrow.core.Either]:
 *
 * `Option<A>` allows us to model absence and has two possible states, `Some(a: A)` if the value is not absent and `None` to represent an empty case.
 * In a similar fashion `Either<L, R>` may have two possible cases `Left(l: L)` and `Right(r: R)`. By convention, `Left` is used to model the exceptional
 * case and `Right` for the successful case.
 *
 * Both [arrow.core.Either] and [arrow.core.Option] are examples of data types that can be computed over transforming their inner results.
 *
 * ```kotlin:ank:playground
 * import arrow.*
 * import arrow.core.*
 *
 * suspend fun main(args: Array<String>) {
 *   val result =
 *   //sampleStart
 *   Either.catch { "1".toInt() }.map { it * 2 }
 *   //sampleEnd
 *   println(result)
 * }
 * ```
 *
 * ```kotlin:ank:playground
 * import arrow.*
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   val result =
 *   //sampleStart
 *   Option(1).map { it * 2 }
 *   //sampleEnd
 *   println(result)
 * }
 * ```
 *
 */
@documented
interface Functor<F> : Invariant<F> {

  fun <A, B> Kind<F, A>.map(f: (A) -> B): Kind<F, B>

  override fun <A, B> Kind<F, A>.imap(f: (A) -> B, g: (B) -> A): Kind<F, B> =
    map(f)

  fun <A, B> lift(f: (A) -> B): (Kind<F, A>) -> Kind<F, B> = { fa: Kind<F, A> ->
    fa.map(f)
  }

  fun <A> Kind<F, A>.void(): Kind<F, Unit> =
    map { Unit }

  fun <A, B> Kind<F, A>.fproduct(f: (A) -> B): Kind<F, Tuple2<A, B>> =
    map { a -> Tuple2(a, f(a)) }

  fun <A, B> Kind<F, A>.mapConst(b: B): Kind<F, B> =
    map { b }

  /**
   * Replaces the [B] value inside [F] with [A] resulting in a Kind<F, A>
   */
  fun <A, B> A.mapConst(fb: Kind<F, B>): Kind<F, A> =
    fb.mapConst(this)

  fun <A, B> Kind<F, A>.tupleLeft(b: B): Kind<F, Tuple2<B, A>> =
    map { a -> Tuple2(b, a) }

  fun <A, B> Kind<F, A>.tupleRight(b: B): Kind<F, Tuple2<A, B>> =
    map { a -> Tuple2(a, b) }

  fun <B, A : B> Kind<F, A>.widen(): Kind<F, B> =
    this
}
