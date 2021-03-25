package arrow.core

import arrow.typeclasses.Semigroup

inline fun <A> identity(a: A): A = a

inline fun <A, B, Z> ((A, B) -> Z).curry(): (A) -> (B) -> Z = { p1: A -> { p2: B -> this(p1, p2) } }

infix fun <A, B, C> ((B) -> C).compose(f: (A) -> B): (A) -> C =
  AndThen(this).compose(f)

infix fun <A, B, C> ((A) -> B).andThen(g: (B) -> C): (A) -> C =
  AndThen(this).andThen(g)

@PublishedApi
internal object ArrowCoreInternalException : RuntimeException(
  "Arrow-Core internal error. Please let us know and create a ticket at https://github.com/arrow-kt/arrow-core/issues/new/choose",
  null
) {
  override fun fillInStackTrace(): Throwable = this
}

const val TailRecMDeprecation: String =
  "tailRecM is deprecated together with the Kind type classes since it's meant for writing kind-based polymorphic stack-safe programs."

/**
 * This is a work-around for having nested nulls in generic code.
 * This allows for writing faster generic code instead of using `Option`.
 * This is only used as an optimisation technique in low-level code,
 * always prefer to use `Option` in actual business code when needed in generic code.
 */
@PublishedApi
internal object EmptyValue {
  @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
  inline fun <A> unbox(value: Any?): A =
    if (value === this) null as A else value as A
}

/**
 * Like [Semigroup.maybeCombine] but for using with [EmptyValue]
 */
@PublishedApi
internal fun <T> Semigroup<T>.emptyCombine(first: Any?, second: T): T =
  if (first == EmptyValue) second else (first as T).combine(second)
