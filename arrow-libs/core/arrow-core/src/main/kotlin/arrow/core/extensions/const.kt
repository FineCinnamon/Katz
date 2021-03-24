package arrow.core.extensions

import arrow.Kind
import arrow.core.Const
import arrow.core.ConstOf
import arrow.core.ConstPartialOf
import arrow.core.Eval
import arrow.core.Option
import arrow.core.Ordering
import arrow.core.Tuple2
import arrow.core.extensions.const.eq.eq
import arrow.core.fix
import arrow.core.value
import arrow.typeclasses.Applicative
import arrow.typeclasses.Apply
import arrow.typeclasses.Contravariant
import arrow.typeclasses.Divide
import arrow.typeclasses.Divisible
import arrow.typeclasses.Eq
import arrow.typeclasses.EqDeprecation
import arrow.typeclasses.EqK
import arrow.typeclasses.Foldable
import arrow.typeclasses.Functor
import arrow.typeclasses.Hash
import arrow.typeclasses.HashDeprecation
import arrow.typeclasses.Invariant
import arrow.typeclasses.Monoid
import arrow.typeclasses.Order
import arrow.typeclasses.OrderDeprecation
import arrow.typeclasses.Semigroup
import arrow.typeclasses.Show
import arrow.typeclasses.ShowDeprecation
import arrow.typeclasses.Traverse
import arrow.typeclasses.TraverseDeprecation
import arrow.typeclasses.TraverseFilter
import arrow.core.combine as combineAp

@Deprecated(
  message = "Invariant typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstInvariant<A> : Invariant<ConstPartialOf<A>> {
  override fun <T, U> ConstOf<A, T>.imap(f: (T) -> U, g: (U) -> T): Const<A, U> =
    fix().retag()
}

@Deprecated(
  message = "Contravariant typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstContravariant<A> : Contravariant<ConstPartialOf<A>> {
  override fun <T, U> ConstOf<A, T>.contramap(f: (U) -> T): Const<A, U> =
    fix().retag()
}

@Deprecated(
  message = "Divide typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstDivideInstance<O> : Divide<ConstPartialOf<O>>, ConstContravariant<O> {
  fun MO(): Monoid<O>
  override fun <A, B, Z> divide(fa: Kind<ConstPartialOf<O>, A>, fb: Kind<ConstPartialOf<O>, B>, f: (Z) -> Tuple2<A, B>): Kind<ConstPartialOf<O>, Z> =
    Const(
      MO().run { fa.value() + fb.value() }
    )
}

@Deprecated(
  message = "Divisible typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstDivisibleInstance<O> : Divisible<ConstPartialOf<O>>, ConstDivideInstance<O> {
  fun MOO(): Monoid<O>
  override fun MO(): Monoid<O> = MOO()

  override fun <A> conquer(): Kind<ConstPartialOf<O>, A> =
    Const(MOO().empty())
}

@Deprecated(
  message = "Functor typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstFunctor<A> : Functor<ConstPartialOf<A>> {
  override fun <T, U> ConstOf<A, T>.map(f: (T) -> U): Const<A, U> =
    fix().retag()
}

@Deprecated(
  message = "Apply typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstApply<A> : Apply<ConstPartialOf<A>> {

  fun MA(): Monoid<A>

  override fun <T, U> ConstOf<A, T>.map(f: (T) -> U): Const<A, U> = fix().retag()

  override fun <T, U> ConstOf<A, T>.ap(ff: ConstOf<A, (T) -> U>): Const<A, U> =
    fix().zip<(T) -> U, U>(MA(), ff.fix()) { a, f -> f(a) }
}

@Deprecated(
  message = "Applicative typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstApplicative<A> : Applicative<ConstPartialOf<A>> {

  fun MA(): Monoid<A>

  override fun <T, U> ConstOf<A, T>.map(f: (T) -> U): Const<A, U> = fix().retag()

  override fun <T> just(a: T): Const<A, T> = object : ConstMonoid<A, T> {
    override fun SA(): Semigroup<A> = MA()
    override fun MA(): Monoid<A> = this@ConstApplicative.MA()
  }.empty().fix()

  override fun <T, U> ConstOf<A, T>.ap(ff: ConstOf<A, (T) -> U>): Const<A, U> =
    fix().zip<(T) -> U, U>(MA(), ff.fix()) { a, f -> f(a) }
}

@Deprecated(
  message = "Foldable typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstFoldable<A> : Foldable<ConstPartialOf<A>> {

  override fun <T, U> ConstOf<A, T>.foldLeft(b: U, f: (U, T) -> U): U = b

  override fun <T, U> ConstOf<A, T>.foldRight(lb: Eval<U>, f: (T, Eval<U>) -> Eval<U>): Eval<U> = lb
}

@Deprecated(
  message = "Traverse typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstTraverse<X> : Traverse<ConstPartialOf<X>>, ConstFoldable<X> {

  override fun <T, U> ConstOf<X, T>.map(f: (T) -> U): Const<X, U> = fix().retag()

  override fun <G, A, B> ConstOf<X, A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, ConstOf<X, B>> =
    fix().traverse(AP, f)
}

@Deprecated(
  message = "TraverseFilter typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstTraverseFilter<X> : TraverseFilter<ConstPartialOf<X>>, ConstTraverse<X> {

  override fun <T, U> Kind<ConstPartialOf<X>, T>.map(f: (T) -> U): Const<X, U> = fix().retag()

  @Deprecated(TraverseDeprecation)
  override fun <G, A, B> Kind<ConstPartialOf<X>, A>.traverseFilter(AP: Applicative<G>, f: (A) -> Kind<G, Option<B>>): Kind<G, ConstOf<X, B>> =
    fix().traverseFilter(AP, f)
}

@Deprecated(
  "Typeclass instance have been moved to the companion object of the typeclass.",
  ReplaceWith("Semigroup.const()", "arrow.typeclasses.Semigroup"),
  DeprecationLevel.WARNING
)
interface ConstSemigroup<A, T> : Semigroup<ConstOf<A, T>> {

  fun SA(): Semigroup<A>

  override fun ConstOf<A, T>.combine(b: ConstOf<A, T>): Const<A, T> =
    combineAp(SA(), b)
}

@Deprecated(
  "Typeclass instance have been moved to the companion object of the typeclass.",
  ReplaceWith("Monoid.const()", "arrow.typeclasses.Monoid"),
  DeprecationLevel.WARNING
)
interface ConstMonoid<A, T> : Monoid<ConstOf<A, T>>, ConstSemigroup<A, T> {

  fun MA(): Monoid<A>

  override fun SA(): Semigroup<A> = MA()

  override fun empty(): Const<A, T> = Const(MA().empty())
}

@Deprecated(EqDeprecation)
interface ConstEq<A, T> : Eq<Const<A, T>> {

  fun EQ(): Eq<A>

  override fun Const<A, T>.eqv(b: Const<A, T>): Boolean =
    EQ().run { value().eqv(b.value()) }
}

@Deprecated(OrderDeprecation)
interface ConstOrder<A, T> : Order<Const<A, T>> {
  fun ORD(): Order<A>
  override fun Const<A, T>.compare(b: Const<A, T>): Ordering =
    ORD().run { value().compare(b.value()) }
}

@Deprecated(
  message = "EqK typeclass is deprecated and will be removed in 0.13.0. Use concrete methods on Const",
  level = DeprecationLevel.WARNING
)
interface ConstEqK<A> : EqK<ConstPartialOf<A>> {

  fun EQA(): Eq<A>

  override fun <T> Kind<ConstPartialOf<A>, T>.eqK(other: Kind<ConstPartialOf<A>, T>, EQ: Eq<T>): Boolean =
    (this.fix() to other.fix()).let {
      Const.eq<A, T>(EQA()).run {
        it.first.eqv(it.second)
      }
    }
}

@Deprecated(ShowDeprecation)
interface ConstShow<A, T> : Show<Const<A, T>> {
  fun SA(): Show<A>
  override fun Const<A, T>.show(): String = show(SA())
}

@Deprecated(HashDeprecation)
interface ConstHash<A, T> : Hash<Const<A, T>> {
  fun HA(): Hash<A>

  override fun Const<A, T>.hashWithSalt(salt: Int): Int = HA().run { value().hashWithSalt(salt) }
}
