package arrow.mtl

import arrow.Kind
import arrow.core.Const
import arrow.core.ConstPartialOf
import arrow.core.Either
import arrow.core.ForConst
import arrow.core.ForId
import arrow.core.Id
import arrow.core.Left
import arrow.core.Option
import arrow.core.Right
import arrow.core.const
import arrow.core.extensions.const.divisible.divisible
import arrow.core.extensions.id.applicative.applicative
import arrow.core.extensions.id.functor.functor
import arrow.core.extensions.id.monad.monad
import arrow.core.extensions.id.traverse.traverse
import arrow.core.extensions.monoid
import arrow.core.extensions.option.functor.functor
import arrow.core.fix
import arrow.core.value
import arrow.fx.ForIO
import arrow.fx.IO
import arrow.fx.extensions.io.applicativeError.attempt
import arrow.fx.extensions.io.async.async
import arrow.fx.mtl.eithert.async.async
import arrow.fx.typeclasses.seconds
import arrow.mtl.extensions.eithert.alternative.alternative
import arrow.mtl.extensions.eithert.applicative.applicative
import arrow.mtl.extensions.eithert.divisible.divisible
import arrow.mtl.extensions.eithert.functor.functor
import arrow.mtl.extensions.eithert.semigroupK.semigroupK
import arrow.mtl.extensions.eithert.traverse.traverse
import arrow.test.UnitSpec
import arrow.test.generators.intSmall
import arrow.test.laws.AlternativeLaws
import arrow.test.laws.AsyncLaws
import arrow.test.laws.DivisibleLaws
import arrow.test.laws.SemigroupKLaws
import arrow.test.laws.TraverseLaws
import arrow.typeclasses.Eq
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

class EitherTTest : UnitSpec() {

  fun <A> EQ(): Eq<Kind<EitherTPartialOf<ForIO, Throwable>, A>> = Eq { a, b ->
    a.value().attempt().unsafeRunTimed(60.seconds) == b.value().attempt().unsafeRunTimed(60.seconds)
  }

  fun GEN() =
    Gen.constant(
      EitherT.applicative<ForId, Int>(Id.monad()))

  init {

    val cf: (Int) -> EitherT<Kind<ForConst, Int>, Int, Int> = { EitherT(it.const()) }
    val g = Gen.int().map(cf) as Gen<Kind<EitherTPartialOf<ConstPartialOf<Int>, Int>, Int>>

    val cfId: (Int) -> EitherT<ForId, Nothing, Int> = { EitherT(Id(Right(it))) }
    val genId = Gen.intSmall().map(cfId) as Gen<Kind<EitherTPartialOf<ForId, Int>, Int>>

    testLaws(
      DivisibleLaws.laws(
        EitherT.divisible<ConstPartialOf<Int>, Int>(Const.divisible(Int.monoid())),
        g,
        Eq { a, b -> a.value().value() == b.value().value() }
      ),
      AlternativeLaws.laws(
        EitherT.alternative(Id.monad(), Int.monoid()),
        { EitherT.just(Id.applicative(), it) },
        { i -> EitherT.just(Id.applicative(), { j: Int -> i + j }) },
        Eq { a, b ->
          a.value().fix() == b.value().fix()
        }
      ),
      AsyncLaws.laws(EitherT.async(IO.async()), EQ(), EQ()),
      TraverseLaws.laws(EitherT.traverse<ForId, Int>(Id.traverse()), EitherT.functor<ForId, Int>(Id.functor()), genId, Eq.any()),
      SemigroupKLaws.laws(
        EitherT.semigroupK<ForId, Int>(Id.monad()),
        GEN() as Gen<Kind<EitherTPartialOf<ForId, Int>, Int>>,
        Eq.any())
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
