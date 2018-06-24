package arrow.optics

import arrow.data.NonEmptyList
import arrow.data.fix

/**
 * [Lens] to operate on the head of a [NonEmptyList]
 */
fun <A> NonEmptyList.Companion.head(): Lens<NonEmptyList<A>, A> = Lens(
  get = NonEmptyList<A>::head,
  set = { newHead -> { nel -> NonEmptyList(newHead, nel.tail) } }
)

/**
 * [Lens] to operate on the tail of a [NonEmptyList]
 */
fun <A> NonEmptyList.Companion.tail(): Lens<NonEmptyList<A>, List<A>> = Lens(
  get = NonEmptyList<A>::tail,
  set = { newTail -> { nel -> NonEmptyList(nel.head, newTail) } }
)
