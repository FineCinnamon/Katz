package arrow.instances

import arrow.Kind
import arrow.core.*
import arrow.deprecation.ExtensionsDSLDeprecated
import arrow.extension
import arrow.typeclasses.*
import arrow.instances.traverse as idTraverse

@extension
interface IdSemigroupInstance<A> : Semigroup<Id<A>> {
  fun SA(): Semigroup<A>

  override fun Id<A>.combine(b: Id<A>): Id<A> {
    return Id(SA().run { value().combine(b.value()) })
  }
}

@extension
interface IdMonoidInstance<A> : Monoid<Id<A>>, IdSemigroupInstance<A> {
  fun MA(): Monoid<A>
  override fun SA(): Semigroup<A> = MA()

  override fun empty(): Id<A> = Id(MA().run { empty() })
}

@extension
interface IdEqInstance<A> : Eq<Id<A>> {

  fun EQ(): Eq<A>

  override fun Id<A>.eqv(b: Id<A>): Boolean =
    EQ().run { value().eqv(b.value()) }
}

@extension
interface IdShowInstance<A> : Show<Id<A>> {
  override fun Id<A>.show(): String =
    toString()
}

@extension
interface IdSemigroupInstance<A> : Semigroup<Id<A>> {

  fun SA(): Semigroup<A>

  override fun Id<A>.combine(b: Id<A>): Id<A> = SA().run {
    Id(value().combine(b.value()))
  }

}

@extension
interface IdMonoidInstance<A> : Monoid<Id<A>>, IdSemigroupInstance<A> {

  fun MA(): Monoid<A>

  override fun SA() = MA()

  override fun empty(): Id<A> =
    Id(MA().empty())

}

@extension
interface IdFunctorInstance : Functor<ForId> {
  override fun <A, B> IdOf<A>.map(f: (A) -> B): Id<B> =
    fix().map(f)
}

@extension
interface IdApplicativeInstance : Applicative<ForId> {
  override fun <A, B> IdOf<A>.ap(ff: IdOf<(A) -> B>): Id<B> =
    fix().ap(ff)

  override fun <A, B> IdOf<A>.map(f: (A) -> B): Id<B> =
    fix().map(f)

  override fun <A> just(a: A): Id<A> =
    Id.just(a)
}

@extension
interface IdMonadInstance : Monad<ForId> {
  override fun <A, B> IdOf<A>.ap(ff: IdOf<(A) -> B>): Id<B> =
    fix().ap(ff)

  override fun <A, B> IdOf<A>.flatMap(f: (A) -> IdOf<B>): Id<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, IdOf<Either<A, B>>>): Id<B> =
    Id.tailRecM(a, f)

  override fun <A, B> IdOf<A>.map(f: (A) -> B): Id<B> =
    fix().map(f)

  override fun <A> just(a: A): Id<A> =
    Id.just(a)
}

@extension
interface IdComonadInstance : Comonad<ForId> {
  override fun <A, B> IdOf<A>.coflatMap(f: (IdOf<A>) -> B): Id<B> =
    fix().coflatMap(f)

  override fun <A> IdOf<A>.extract(): A =
    fix().extract()

  override fun <A, B> IdOf<A>.map(f: (A) -> B): Id<B> =
    fix().map(f)
}

@extension
interface IdBimonadInstance : Bimonad<ForId> {
  override fun <A, B> IdOf<A>.ap(ff: IdOf<(A) -> B>): Id<B> =
    fix().ap(ff)

  override fun <A, B> IdOf<A>.flatMap(f: (A) -> IdOf<B>): Id<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, IdOf<Either<A, B>>>): Id<B> =
    Id.tailRecM(a, f)

  override fun <A, B> IdOf<A>.map(f: (A) -> B): Id<B> =
    fix().map(f)

  override fun <A> just(a: A): Id<A> =
    Id.just(a)

  override fun <A, B> IdOf<A>.coflatMap(f: (IdOf<A>) -> B): Id<B> =
    fix().coflatMap(f)

  override fun <A> IdOf<A>.extract(): A =
    fix().extract()
}

@extension
interface IdFoldableInstance : Foldable<ForId> {
  override fun <A, B> IdOf<A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> IdOf<A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    fix().foldRight(lb, f)
}

fun <A, G, B> IdOf<A>.traverse(GA: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, Id<B>> = GA.run {
  f(value()).map { Id(it) }
}

fun <A, G> IdOf<Kind<G, A>>.sequence(GA: Applicative<G>): Kind<G, Id<A>> =
  idTraverse(GA, ::identity)

@extension
interface IdTraverseInstance : Traverse<ForId> {
  override fun <A, B> IdOf<A>.map(f: (A) -> B): Id<B> =
    fix().map(f)

  override fun <G, A, B> IdOf<A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, Id<B>> =
    idTraverse(AP, f)

  override fun <A, B> IdOf<A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> arrow.Kind<arrow.core.ForId, A>.foldRight(lb: arrow.core.Eval<B>, f: (A, arrow.core.Eval<B>) -> arrow.core.Eval<B>): Eval<B> =
    fix().foldRight(lb, f)
}

@extension
interface IdHashInstance<A> : Hash<Id<A>>, IdEqInstance<A> {

  fun HA(): Hash<A>

  override fun EQ(): Eq<A> = HA()

  override fun Id<A>.hash(): Int = HA().run { value().hash() }
}

object IdContext : IdBimonadInstance, IdTraverseInstance {
  override fun <A, B> IdOf<A>.map(f: (A) -> B): Id<B> =
    fix().map(f)
}

@Deprecated(ExtensionsDSLDeprecated)
infix fun <L> ForId.Companion.extensions(f: IdContext.() -> L): L =
  f(IdContext)