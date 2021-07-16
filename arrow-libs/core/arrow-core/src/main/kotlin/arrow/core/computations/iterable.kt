package arrow.core.computations

import arrow.continuations.Effect
import arrow.continuations.generic.DelimitedScope

/**
 * Allows to iterate over the [Iterable]<A> receiver and operate on an arbitrary number of items.
 *
 * With some special features:
 * - Not all items need to be consumed.
 * - If we try to get a value after there are no more items then results in a `null` value.
 * - At any point we can cancel the iteration resulting in a `null` value.
 *
 * Can be used to apply operations to an arbitrary number of items in an [Iterable]<A>:
 * ```kotlin:ank
 * val result: String? = listOf(1, 2, 3).iterateOrNull {
 *    val a = next() // gets 1
 *    val b = next() // gets 2
 *    "$a $b"
 * }
 * result shouldBe "1 2"
 * ```
 *
 * It's possible to also skip a value:
 * ```kotlin:ank
 * val result: String? = listOf(1, 2, 3).iterateOrNull {
 *    val a = next() // gets 1
 *    dropNext() // drop 2
 *    val b = next() //gets 3
 *    "$a $b"
 * }
 * result shouldBe "1 3"
 * ```
 *
 * If we need to skip more than one:
 * ```kotlin:ank
 * val result: String? = listOf(1, 2, 3, 4).iterateOrNull {
 *    val a = next() // gets 1
 *    drop(2) // drops 2 and 3
 *    val b = next() // gets 4
 *    "$a $b"
 * }
 * result shouldBe "1 4"
 * ```
 *
 * We can use `cancel()` to end the iteration if a given condition is met:
 * ```kotlin:ank
 * val result: String? = listOf(1, 2, 3, 4).iterateOrNull {
 *    val a = next() // gets 1
 *    if(a == 1) {
 *      cancel() // ends iteration
 *    }
 *    val b = next() // is never run
 *    "$a $b" // won't return
 * }
 * result shouldBe null
 * ```
 */
suspend fun <A, R> Iterable<A>.iterateOrNull(
  block: suspend IterableEffect<A, R?>.() -> R,
): R? = Effect.suspended(eff = { IterableEffect(it, iterator()) }, f = block, just = { it })

class IterableEffect<A, R>(
  private val delimitedScope: DelimitedScope<R?>,
  private val iterator: Iterator<A>,
) : Effect<R?> {

  /**
   * returns the next items in the current iteration if available.
   */
  suspend fun next(): A = iterator.takeIf { it.hasNext() }?.next() ?: cancel()

  /**
   * Drops the next item in the current iteration without returning the value, alias of [next].
   */
  suspend fun dropNext() {
    next()
  }

  /**
   * Drops [n] items from the current iteration.
   */
  suspend fun drop(n: Int) = repeat(n) { dropNext() }

  /**
   * Cancels the current iteration returning null and skipping any further operations.
   */
  suspend fun <B> cancel(): B = control().shift(null)

  override fun control(): DelimitedScope<R?> = delimitedScope
}
