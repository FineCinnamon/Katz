package arrow.optics

import arrow.core.Either
import arrow.core.fix
import arrow.data.Invalid
import arrow.data.Valid
import arrow.data.Validated
import arrow.data.fix

/**
 * [PIso] that defines the equality between [Either] and [Validated]
 */
fun <A1, A2, B1, B2> Either.Companion.toPValidated(): PIso<Either<A1, B1>, Either<A2, B2>, Validated<A1, B1>, Validated<A2, B2>> = PIso(
  get = { it.fold(::Invalid, ::Valid) },
  reverseGet = Validated<A2, B2>::toEither
)

/**
 * [Iso] that defines the equality between [Either] and [Validated]
 */
fun <A, B> Either.Companion.toValidated(): Iso<Either<A, B>, Validated<A, B>> = toPValidated()