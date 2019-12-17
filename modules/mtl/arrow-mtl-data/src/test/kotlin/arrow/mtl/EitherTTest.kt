package arrow.mtl

import arrow.Kind
import arrow.core.Const
import arrow.core.ConstPartialOf
import arrow.core.Either
import arrow.core.ForId
import arrow.core.Id
import arrow.core.Left
import arrow.core.Option
import arrow.core.Right
import arrow.core.extensions.const.divisible.divisible
import arrow.core.extensions.id.applicative.applicative
import arrow.core.extensions.id.monad.monad
import arrow.core.extensions.id.traverse.traverse
import arrow.core.extensions.monoid
import arrow.core.extensions.option.functor.functor
import arrow.core.fix
import arrow.core.value
import arrow.fx.ForIO
import arrow.fx.IO
import arrow.fx.extensions.io.applicativeError.attempt
import arrow.fx.extensions.io.concurrent.concurrent
import arrow.fx.mtl.concurrent
import arrow.fx.typeclasses.seconds
import arrow.mtl.extensions.eithert.alternative.alternative
import arrow.mtl.extensions.eithert.divisible.divisible
import arrow.mtl.extensions.eithert.semigroupK.semigroupK
import arrow.mtl.extensions.eithert.traverse.traverse
import arrow.test.UnitSpec
import arrow.test.generators.genK
import arrow.test.laws.AlternativeLaws
import arrow.test.laws.ConcurrentLaws
import arrow.test.laws.DivisibleLaws
import arrow.test.laws.SemigroupKLaws
import arrow.test.laws.TraverseLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.EqK
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

class EitherTTest : UnitSpec() {

  fun <E, A> EQ(): Eq<Kind<EitherTPartialOf<ForIO, E>, A>> = Eq { a, b ->
    a.value().attempt().unsafeRunTimed(60.seconds) == b.value().attempt().unsafeRunTimed(60.seconds)
  }

  init {
    val idEQK = object : EqK<EitherTPartialOf<ForId, Int>> {
      override fun <A> Kind<EitherTPartialOf<ForId, Int>, A>.eqK(other: Kind<EitherTPartialOf<ForId, Int>, A>, EQ: Eq<A>): Boolean =
        (this.fix().value().fix() to other.fix().value().fix()).let {
          Eq.any().run { it.first.eqv(it.second) }
        }
    }

    val ioEQK = object : EqK<EitherTPartialOf<ForIO, Throwable>> {
      override fun <A> Kind<EitherTPartialOf<ForIO, Throwable>, A>.eqK(other: Kind<EitherTPartialOf<ForIO, Throwable>, A>, EQ: Eq<A>): Boolean =
        (this.fix() to other.fix()).let {
          ioEQ<A>().run {
            it.first.eqv(it.second)
          }
        }
    }

    val constEQK = object : EqK<EitherTPartialOf<ConstPartialOf<Int>, Int>> {
      override fun <A> Kind<EitherTPartialOf<ConstPartialOf<Int>, Int>, A>.eqK(other: Kind<EitherTPartialOf<ConstPartialOf<Int>, Int>, A>, EQ: Eq<A>): Boolean {
        return this.value().value() == other.value().value()
      }
    }

    testLaws(
      DivisibleLaws.laws(
        EitherT.divisible<ConstPartialOf<Int>, Int>(Const.divisible(Int.monoid())),
        EitherT.genK(Const.genK(Gen.int()), Gen.int()),
        constEQK
      ),
      AlternativeLaws.laws(
        EitherT.alternative(Id.monad(), Int.monoid()),
        { EitherT.just(Id.applicative(), it) },
        { i -> EitherT.just(Id.applicative(), { j: Int -> i + j }) },
        idEQK
      ),
      ConcurrentLaws.laws<EitherTPartialOf<ForIO, String>>(EitherT.concurrent(IO.concurrent()), EQ(), EQ(), EQ()),
      TraverseLaws.laws(EitherT.traverse<ForId, Int>(Id.traverse()),
        EitherT.genK(Id.genK(), Gen.int()),
        idEQK),
      SemigroupKLaws.laws(
        EitherT.semigroupK<ForId, Int>(Id.monad()),
        EitherT.genK(Id.genK(), Gen.int()),
        idEQK
      )
    )

    "mapLeft should alter left instance only" {
      forAll { i: Int, j: Int ->
        val left: Either<Int, Int> = Left(i)
        val right: Either<Int, Int> = Right(j)
        EitherT(Option(left)).mapLeft(Option.functor()) { it + 1 } == EitherT(Option(Left(i + 1))) &&
          EitherT(Option(right)).mapLeft(Option.functor()) { it + 1 } == EitherT(Option(right)) &&
          EitherT(Option.empty<Either<Int, Int>>()).mapLeft(Option.functor()) { it + 1 } == EitherT(Option.empty<Either<Int, Int>>())
      }
    }
  }
}
