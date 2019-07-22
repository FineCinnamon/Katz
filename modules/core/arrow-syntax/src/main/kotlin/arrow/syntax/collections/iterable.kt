package arrow.syntax.collections

import arrow.core.Option
import arrow.core.PartialFunction
import arrow.core.Predicate
import arrow.core.andThen
import arrow.core.getOrElse
import arrow.core.orElse
import arrow.core.toOption

@Deprecated(message = "`firstOption` is now part of the Foldable interface and generalized to all foldable data types")
fun <T> Iterable<T>.firstOption(): Option<T> = firstOrNull().toOption()

@Deprecated(message = "`firstOption` is now part of the Foldable interface and generalized to all foldable data types")
fun <T> Iterable<T>.firstOption(predicate: Predicate<T>): Option<T> = firstOrNull(predicate).toOption()

fun <A : Any, B> Iterable<A>.collect(vararg cases: (A) -> Option<B>): List<B> =
  flatMap { value: A ->
    val f: (A) -> Option<B> = cases.reduce { f: (A) -> Option<B>, g: (A) -> Option<B> ->
      f.andThen { optionB -> optionB.getOrElse { g(value) } } as (A) -> Option<B>
    }
    f(value).map { listOf(it) }.getOrElse { emptyList() }
  }
// flatMap { value: A ->
//   val f: (A) -> Option<B> = cases.reduce { a, b -> a.orElse(b) }
//   if (f.isDefinedAt(value)) listOf(f(value))
//   else emptyList()
// }
