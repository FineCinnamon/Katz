package arrow.core.extensions

import arrow.Kind
import arrow.core.ForTry
import arrow.core.Option
import arrow.core.Try
import arrow.core.fix
import arrow.extension
import arrow.typeclasses.FunctorFilter

@extension
interface TryFunctorFilter : FunctorFilter<ForTry> {

  override fun <A, B> Kind<ForTry, A>.filterMap(f: (A) -> Option<B>): Try<B> =
    fix().filterMap(f)

  override fun <A, B> Kind<ForTry, A>.map(f: (A) -> B): Try<B> =
    fix().map(f)
}
