package arrow.mtl.extensions

import arrow.Kind
import arrow.Kind2
import arrow.core.Either
import arrow.core.toT
import arrow.extension
import arrow.mtl.AccumT
import arrow.mtl.AccumTPartialOf
import arrow.mtl.ForAccumT
import arrow.mtl.fix
import arrow.mtl.typeclasses.MonadTrans
import arrow.typeclasses.Alternative
import arrow.typeclasses.Applicative
import arrow.typeclasses.ApplicativeError
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadError
import arrow.typeclasses.Monoid

@extension
interface AccumTFunctor<S, F> : Functor<AccumTPartialOf<S, F>> {

  fun FF(): Functor<F>

  override fun <A, B> Kind<AccumTPartialOf<S, F>, A>.map(f: (A) -> B): Kind<AccumTPartialOf<S, F>, B> =
    this.fix().map(FF(), f)
}

@extension
interface AccumTApplicative<S, F> : Applicative<AccumTPartialOf<S, F>> {
  fun MS(): Monoid<S>
  fun MF(): Monad<F>

  override fun <A> just(a: A): Kind<AccumTPartialOf<S, F>, A> =
    AccumT.just(MS(), MF(), a)

  override fun <A, B> Kind<AccumTPartialOf<S, F>, A>.ap(ff: Kind<AccumTPartialOf<S, F>, (A) -> B>): Kind<AccumTPartialOf<S, F>, B> =
    fix().ap(MS(), MF(), ff)
}

@extension
interface AccumTMonad<S, F> : Monad<AccumTPartialOf<S, F>>, AccumTApplicative<S, F> {

  override fun MS(): Monoid<S>
  override fun MF(): Monad<F>

  override fun <A> just(a: A): Kind<AccumTPartialOf<S, F>, A> =
    AccumT.just(MS(), MF(), a)

  override fun <A, B> Kind<AccumTPartialOf<S, F>, A>.flatMap(f: (A) -> Kind<AccumTPartialOf<S, F>, B>): Kind<AccumTPartialOf<S, F>, B> =
    this.fix().flatMap(MS(), MF(), f)

  override fun <A, B> tailRecM(a: A, f: (A) -> Kind<AccumTPartialOf<S, F>, Either<A, B>>): Kind<AccumTPartialOf<S, F>, B> =
    AccumT.tailRecM(MF(), a, f)

  override fun <A, B> Kind<AccumTPartialOf<S, F>, A>.ap(ff: Kind<AccumTPartialOf<S, F>, (A) -> B>): Kind<AccumTPartialOf<S, F>, B> =
    fix().ap(MS(), MF(), ff)
}

@extension
interface AccumtTMonadTrans<S> : MonadTrans<Kind<ForAccumT, S>> {

  fun MS(): Monoid<S>

  override fun <G, A> Kind<G, A>.liftT(MG: Monad<G>): Kind2<Kind<ForAccumT, S>, G, A> =
    AccumT(MG) { _: S ->
      MG.run {
        flatMap { a ->
          MG.just(MS().empty() toT a)
        }
      }
    }
}

@extension
interface AccumTAlternative<S, F> : Alternative<AccumTPartialOf<S, F>>, AccumTApplicative<S, F> {

  fun AF(): Alternative<F>
  override fun MF(): Monad<F>
  override fun MS(): Monoid<S>

  override fun <A> Kind<AccumTPartialOf<S, F>, A>.orElse(b: Kind<AccumTPartialOf<S, F>, A>): Kind<AccumTPartialOf<S, F>, A> =
    (this.fix() to b.fix()).let { (ls, rs) ->
      AccumT(AF()) { s: S ->
        AF().run {
          ls.runAccumT(MF(), s).orElse(rs.runAccumT(MF(), s))
        }
      }
    }

  override fun <A> empty(): Kind<AccumTPartialOf<S, F>, A> =
    AccumT.liftF(AF(), AF().empty())
}

@extension
interface AccumTApplicativeError<S, F, E> : ApplicativeError<AccumTPartialOf<S, F>, E>, AccumTApplicative<S, F> {
  fun ME(): MonadError<F, E>

  override fun MS(): Monoid<S>
  override fun MF(): Monad<F> = ME()

  override fun <A> raiseError(e: E): Kind<AccumTPartialOf<S, F>, A> =
    AccumT.liftF(MF(), ME().raiseError(e))

  override fun <A> Kind<AccumTPartialOf<S, F>, A>.handleErrorWith(f: (E) -> Kind<AccumTPartialOf<S, F>, A>): Kind<AccumTPartialOf<S, F>, A> =
    this.fix().let { accumT ->
      AccumT(MF()) { s: S ->
        ME().run {
          accumT.runAccumT(MF(), s).handleErrorWith { e ->
            f(e).fix().runAccumT(MF(), s)
          }
        }
      }
    }
}

@extension
interface AccumTMonadError<S, F, E> : MonadError<AccumTPartialOf<S, F>, E>, AccumTApplicativeError<S, F, E>, AccumTMonad<S, F> {
  override fun MS(): Monoid<S>
  override fun ME(): MonadError<F, E>
  override fun MF(): Monad<F> = ME()
}

