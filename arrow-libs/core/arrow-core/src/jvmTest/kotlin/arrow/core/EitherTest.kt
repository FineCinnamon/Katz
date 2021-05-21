package arrow.core

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.computations.EitherEffect
import arrow.core.computations.RestrictedEitherEffect
import arrow.core.computations.either
import arrow.core.test.UnitSpec
import arrow.core.test.generators.any
import arrow.core.test.generators.either
import arrow.core.test.generators.intSmall
import arrow.core.test.generators.suspendFunThatReturnsAnyLeft
import arrow.core.test.generators.suspendFunThatReturnsAnyRight
import arrow.core.test.generators.suspendFunThatReturnsEitherAnyOrAnyOrThrows
import arrow.core.test.generators.suspendFunThatThrows
import arrow.core.test.generators.suspendFunThatThrowsFatalThrowable
import arrow.core.test.laws.FxLaws
import arrow.core.test.laws.MonoidLaws
import arrow.typeclasses.Monoid
import io.kotest.property.Arb
Arb.list(
import io.kotest.property.checkAll
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.coroutines.runBlocking

class EitherTest : UnitSpec() {

  val GEN = Gen.either(Gen.string(), Gen.int())

  init {
    testLaws(
      MonoidLaws.laws(Monoid.either(Monoid.string(), Monoid.int()), GEN),
      FxLaws.suspended<EitherEffect<String, *>, Either<String, Int>, Int>(
        Gen.int().map(::Right),
        GEN.map { it },
        Either<String, Int>::equals,
        either::invoke
      ) {
        it.bind()
      },
      FxLaws.eager<RestrictedEitherEffect<String, *>, Either<String, Int>, Int>(
        Gen.int().map(::Right),
        GEN.map { it },
        Either<String, Int>::equals,
        either::eager
      ) {
        it.bind()
      }
    )

    "isLeft should return true if Left and false if Right" {
      forAll { a: Int ->
        Left(a).isLeft() && !Right(a).isLeft()
      }
    }

    "isRight should return false if Left and true if Right" {
      forAll { a: Int ->
        !Left(a).isRight() && Right(a).isRight()
      }
    }

    "fold should apply first op if Left and second op if Right" {
      forAllSmallInt { a: Int, b: Int ->
        val right: Either<Int, Int> = Right(a)
        val left: Either<Int, Int> = Left(b)

        right.fold({ it + 2 }, { it + 1 }) == a + 1 &&
          left.fold({ it + 2 }, { it + 1 }) == b + 2
      }
    }

    "foldLeft should return initial if Left and apply op if Right" {
      forAllSmallInt { a: Int, b: Int, c: Int ->
        Right(a).foldLeft(c, Int::plus) == c + a &&
          Left(b).foldLeft(c, Int::plus) == c
      }
    }

    "foldMap should return the empty of the inner type if Left and apply op if Right" {
      forAllSmallInt { a: Int, b: Int ->
        val left: Either<Int, Int> = Left(b)

        Right(a).foldMap(Monoid.int()) { it + 1 } == a + 1 &&
          left.foldMap(Monoid.int()) { it + 1 } == Monoid.int().empty()
      }
    }

    "bifoldLeft should apply first op if Left and apply second op if Right" {
      forAllSmallInt { a: Int, b: Int, c: Int ->
        Right(a).bifoldLeft(c, Int::plus, Int::times) == a * c &&
          Left(b).bifoldLeft(c, Int::plus, Int::times) == b + c
      }
    }

    "bifoldMap should apply first op if Left and apply second op if Right" {
      forAllSmallInt { a: Int, b: Int ->
        val right: Either<Int, Int> = Right(a)
        val left: Either<Int, Int> = Left(b)

        right.bifoldMap(Monoid.int(), { it + 2 }, { it + 1 }) == a + 1 &&
          left.bifoldMap(Monoid.int(), { it + 2 }, { it + 1 }) == b + 2
      }
    }

    "fromNullable should lift value as a Right if it is not null" {
      forAll { a: Int ->
        Either.fromNullable(a) == Right(a)
      }
    }

    "fromNullable should lift value as a Left(Unit) if it is null" {
      Either.fromNullable(null) shouldBe Left(Unit)
    }

    "empty should return a Right of the empty of the inner type" {
      Right(Monoid.string().empty()) shouldBe Monoid.either(Monoid.string(), Monoid.string()).empty()
    }

    "combine two rights should return a right of the combine of the inners" {
      forAll { a: String, b: String ->
        Monoid.string().run { Right(a.combine(b)) } == Right(a).combine(Monoid.string(), Monoid.string(), Right(b))
      }
    }

    "combine two lefts should return a left of the combine of the inners" {
      forAll { a: String, b: String ->
        Monoid.string().run { Left(a.combine(b)) } == Left(a).combine(Monoid.string(), Monoid.string(), Left(b))
      }
    }

    "combine a right and a left should return left" {
      forAll { a: String, b: String ->
        Left(a) == Left(a).combine(Monoid.string(), Monoid.string(), Right(b)) &&
          Left(a) == Right(b).combine(Monoid.string(), Monoid.string(), Left(a))
      }
    }

    "getOrElse should return value" {
      forAll { a: Int, b: Int ->
        Right(a).getOrElse { b } == a &&
          Left(a).getOrElse { b } == b
      }
    }

    "orNull should return value" {
      forAll { a: Int ->
        Right(a).orNull() == a
      }
    }

    "orNone should return Some(value)" {
      forAll { a: Int ->
        Either.Right(a).orNone() == Some(a)
      }
    }

    "orNone should return None when left" {
      forAll { a: String ->
        Left(a).orNone() == None
      }
    }

    "getOrHandle should return value" {
      forAll { a: Int, b: Int ->
        Right(a).getOrHandle { b } == a &&
          Left(a).getOrHandle { it + b } == a + b
      }
    }

    "filterOrElse should filter values" {
      forAllSmallInt { a: Int, b: Int ->
        val left: Either<Int, Int> = Left(a)

        Right(a).filterOrElse({ it > a - 1 }, { b }) == Right(a) &&
          Right(a).filterOrElse({ it > a + 1 }, { b }) == Left(b) &&
          left.filterOrElse({ it > a - 1 }, { b }) == Left(a) &&
          left.filterOrElse({ it > a + 1 }, { b }) == Left(a)
      }
    }

    "filterOrOther should filter values" {
      forAllSmallInt { a: Int, b: Int ->
        val left: Either<Int, Int> = Left(a)

        Right(a).filterOrOther({ it > a - 1 }, { b + a }) == Right(a) &&
          Right(a).filterOrOther({ it > a + 1 }, { b + a }) == Left(b + a) &&
          left.filterOrOther({ it > a - 1 }, { b + a }) == Left(a) &&
          left.filterOrOther({ it > a + 1 }, { b + a }) == Left(a)
      }
    }

    "leftIfNull should return Left if Right value is null of if Either is Left" {
      forAll { a: Int, b: Int ->
        Right(a).leftIfNull { b } == Right(a) &&
          Right(null).leftIfNull { b } == Left(b) &&
          Left(a).leftIfNull { b } == Left(a)
      }
    }

    "exists should apply predicate to Right only" {
      forAllSmallInt { a: Int ->
        val left: Either<Int, Int> = Left(a)

        Right(a).exists { it > a - 1 } &&
          !Right(a).exists { it > a + 1 } &&
          !left.exists { it > a - 1 } &&
          !left.exists { it > a + 1 }
      }
    }

    "rightIfNotNull should return Left if value is null or Right of value when not null" {
      forAll { a: Int, b: Int ->
        null.rightIfNotNull { b } == Left(b) &&
          a.rightIfNotNull { b } == Right(a)
      }
    }

    "rightIfNull should return Left if value is not null or Right of value when null" {
      forAll { a: Int, b: Int ->
        a.rightIfNull { b } == Left(b) &&
          null.rightIfNull { b } == Right(null)
      }
    }

    "swap should interchange values" {
      forAll { a: Int ->
        Left(a).swap() == Right(a) &&
          Right(a).swap() == Left(a)
      }
    }

    "orNull should convert" {
      forAll { a: Int ->
        val left: Either<Int, Int> = Left(a)

        Right(a).orNull() == a &&
          left.orNull() == null
      }
    }

    "contains should check value" {
      forAllSmallInt { a: Int, b: Int ->
        val rightContains = Right(a).contains(a)
        // We need to check that a != b or this test will result in a false negative
        val rightDoesntContains = if (a != b) !Right(a).contains(b) else true
        val leftNeverContains = !Left(a).contains(a)

        rightContains && rightDoesntContains && leftNeverContains
      }
    }

    "map should alter right instance only" {
      forAllSmallInt { a: Int, b: Int ->
        val right: Either<Int, Int> = Right(a)
        val left: Either<Int, Int> = Left(b)

        right.map { it + 1 } == Right(a + 1) && left.map { it + 1 } == left
      }
    }

    "mapLeft should alter left instance only" {
      forAllSmallInt { a: Int, b: Int ->
        val right: Either<Int, Int> = Right(a)
        val left: Either<Int, Int> = Left(b)

        right.mapLeft { it + 1 } == right && left.mapLeft { it + 1 } == Left(b + 1)
      }
    }

    "bimap should alter left or right instance accordingly" {
      forAllSmallInt { a: Int, b: Int ->
        val right: Either<Int, Int> = Right(a)
        val left: Either<Int, Int> = Left(b)

        right.bimap({ it + 2 }, { it + 1 }) == Right(a + 1) &&
          left.bimap({ it + 2 }, { it + 1 }) == Left(b + 2)
      }
    }

    "replicate should return Right(empty list) when n <= 0" {
      checkAll(
        Gen.oneOf(Gen.negativeIntegers(), Gen.constant(0)),
        Gen.int()
      ) { n: Int, a: Int ->
        val expected: Either<Int, List<Int>> = Right(emptyList())

        Right(a).replicate(n) == expected &&
          Left(a).replicate(n) == expected
      }
    }

    "replicate should return Right(list of repeated value size n) when Right and n is positive" {
      checkAll(
        Gen.intSmall().filter { it > 0 },
        Gen.int()
      ) { n: Int, a: Int ->
        Right(a).replicate(n) == Right(List(n) { a }) &&
          Left(a).replicate(n) == Left(a)
      }
    }

    "traverse should return list of Right when Right and empty list when Left" {
      checkAll(
        Gen.int(),
        Gen.int(),
        Gen.int()
      ) { a: Int, b: Int, c: Int ->
        Right(a).traverse { emptyList<Int>() } == emptyList<Int>() &&
          Right(a).traverse { listOf(b, c) } == listOf(Right(b), Right(c)) &&
          Left(a).traverse { listOf(b, c) } == emptyList<Int>()
      }
    }

    "flatMap should map right instance only" {
      forAllSmallInt { a: Int, b: Int ->
        val right: Either<Int, Int> = Right(a)
        val left: Either<Int, Int> = Left(b)

        right.flatMap { Right(it + 1) } == Right(a + 1) &&
          left.flatMap { Right(it + 1) } == left
      }
    }

    "conditionally should create right instance only if test is true" {
      forAll { t: Boolean, i: Int, s: String ->
        val expected = if (t) Right(i) else Left(s)
        Either.conditionally(t, { s }, { i }) == expected
      }
    }

    "handleErrorWith should handle left instance otherwise return Right" {
      forAll { a: Int, b: String ->
        Left(a).handleErrorWith { Right(b) } == Right(b) &&
          Right(a).handleErrorWith { Right(b) } == Right(a) &&
          Left(a).handleErrorWith { Left(b) } == Left(b)
      }
    }

    "catch should return Right(result) when f does not throw" {
      suspend fun loadFromNetwork(): Int = 1
      Either.catch { loadFromNetwork() } shouldBe Right(1)
    }

    "catch should return Left(result) when f throws" {
      val exception = Exception("Boom!")
      suspend fun loadFromNetwork(): Int = throw exception
      Either.catch { loadFromNetwork() } shouldBe Left(exception)
    }

    "catchAndFlatten should return Right(result) when f does not throw" {
      suspend fun loadFromNetwork(): Either<Throwable, Int> = Right(1)
      Either.catchAndFlatten { loadFromNetwork() } shouldBe Right(1)
    }

    "catchAndFlatten should return Left(result) when f throws" {
      val exception = Exception("Boom!")
      suspend fun loadFromNetwork(): Either<Throwable, Int> = throw exception
      Either.catchAndFlatten { loadFromNetwork() } shouldBe Left(exception)
    }

    "resolve should yield a result when deterministic functions are used as handlers" {
      checkAll(
        Gen.suspendFunThatReturnsEitherAnyOrAnyOrThrows(),
        Gen.any()
      ) { f: suspend () -> Either<Any, Any>, returnObject: Any ->

        runBlocking {
          val result =
            Either.resolve(
              f = { f() },
              success = { a -> handleWithPureFunction(a, returnObject) },
              error = { e -> handleWithPureFunction(e, returnObject) },
              throwable = { t -> handleWithPureFunction(t, returnObject) },
              unrecoverableState = { handleWithPureFunction(it) }
            )
          result == returnObject
        }
      }
    }

    "resolve should throw a Throwable when a fatal Throwable is thrown" {
      checkAll(
        Gen.suspendFunThatThrowsFatalThrowable(),
        Gen.any()
      ) { f: suspend () -> Either<Any, Any>, returnObject: Any ->

        runBlocking {
          shouldThrow<Throwable> {
            Either.resolve(
              f = { f() },
              success = { a -> handleWithPureFunction(a, returnObject) },
              error = { e -> handleWithPureFunction(e, returnObject) },
              throwable = { t -> handleWithPureFunction(t, returnObject) },
              unrecoverableState = { handleWithPureFunction(it) }
            )
          }
        }
        true
      }
    }

    "resolve should yield a result when an exception is thrown in the success supplied function" {
      checkAll(
        Gen.suspendFunThatReturnsAnyRight(),
        Gen.any()
      ) { f: suspend () -> Either<Any, Any>, returnObject: Any ->

        runBlocking {
          val result =
            Either.resolve(
              f = { f() },
              success = { throwException(it) },
              error = { e -> handleWithPureFunction(e, returnObject) },
              throwable = { t -> handleWithPureFunction(t, returnObject) },
              unrecoverableState = { handleWithPureFunction(it) }
            )
          result == returnObject
        }
      }
    }

    "resolve should yield a result when an exception is thrown in the error supplied function" {
      checkAll(
        Gen.suspendFunThatReturnsAnyLeft(),
        Gen.any()
      ) { f: suspend () -> Either<Any, Any>, returnObject: Any ->

        runBlocking {
          val result =
            Either.resolve(
              f = { f() },
              success = { a -> handleWithPureFunction(a, returnObject) },
              error = { throwException(it) },
              throwable = { t -> handleWithPureFunction(t, returnObject) },
              unrecoverableState = { handleWithPureFunction(it) }
            )
          result == returnObject
        }
      }
    }

    "resolve should throw a Throwable when any exception is thrown in the throwable supplied function" {
      checkAll(
        Gen.suspendFunThatThrows()
      ) { f: suspend () -> Either<Any, Any> ->

        runBlocking {
          shouldThrow<Throwable> {
            Either.resolve(
              f = { f() },
              success = { throwException(it) },
              error = { throwException(it) },
              throwable = { throwException(it) },
              unrecoverableState = { handleWithPureFunction(it) }
            )
          }
        }
        true
      }
    }

    "traverse should return list if either is right" {
      val right: Either<String, Int> = Right(1)
      val left: Either<String, Int> = Left("foo")

      right.traverse { listOf(it, 2, 3) } shouldBe listOf(Right(1), Right(2), Right(3))
      left.traverse { listOf(it, 2, 3) } shouldBe emptyList()
    }

    "sequence should be consistent with traverse" {
      checkAll(Gen.either(Gen.string(), Gen.int())) { either ->
        either.map { listOf(it) }.sequence() == either.traverse { listOf(it) }
      }
    }

    "traverseOption should return option if either is right" {
      val right: Either<String, Int> = Right(1)
      val left: Either<String, Int> = Left("foo")

      right.traverseOption { Some(it) } shouldBe Some(Right(1))
      left.traverseOption { Some(it) } shouldBe None
    }

    "sequenceOption should be consistent with traverseOption" {
      checkAll(Gen.either(Gen.string(), Gen.int())) { either ->
        either.map { Some(it) }.sequenceOption() == either.traverseOption { Some(it) }
      }
    }

    "traverseValidated should return validated of either" {
      val right: Either<String, Int> = Right(1)
      val left: Either<String, Int> = Left("foo")

      right.traverseValidated { it.valid() } shouldBe Valid(Right(1))
      left.traverseValidated { it.valid() } shouldBe Valid(Left("foo"))
    }

    "sequenceValidated should be consistent with traverseValidated" {
      checkAll(Gen.either(Gen.string(), Gen.int())) { either ->
        either.map { it.valid() }.sequenceValidated() == either.traverseValidated { it.valid() }
      }
    }

    "bitraverse should wrap either in a list" {
      val right: Either<String, Int> = Right(1)
      val left: Either<String, Int> = Left("foo")

      right.bitraverse({ listOf(it, "bar", "baz") }, { listOf(it, 2, 3) }) shouldBe listOf(Right(1), Right(2), Right(3))
      left.bitraverse({ listOf(it, "bar", "baz") }, { listOf(it, 2, 3) }) shouldBe
        listOf(Left("foo"), Left("bar"), Left("baz"))
    }

    "bisequence should be consistent with bitraverse" {
      checkAll(Gen.either(Gen.string(), Gen.int())) { either ->
        either.bimap({ listOf(it) }, { listOf(it) }).bisequence() == either.bitraverse({ listOf(it) }, { listOf(it) })
      }
    }

    "bitraverseOption should wrap either in an option" {
      val right: Either<String, Int> = Right(1)
      val left: Either<String, Int> = Left("foo")

      right.bitraverseOption({ Some(it) }, { Some(it.toString()) }) shouldBe Some(Right("1"))
      left.bitraverseOption({ Some(it) }, { Some(it.toString()) }) shouldBe Some(Left("foo"))
    }

    "bisequenceOption should be consistent with bitraverseOption" {
      checkAll(Gen.either(Gen.string(), Gen.int())) { either ->
        either.bimap({ Some(it) }, { Some(it) }).bisequenceOption() ==
          either.bitraverseOption({ Some(it) }, { Some(it) })
      }
    }

    "bitraverseValidated should return validated of either" {
      val right: Either<String, Int> = Right(1)
      val left: Either<String, Int> = Left("foo")

      right.bitraverseValidated({ it.invalid() }, { it.valid() }) shouldBe Valid(Right(1))
      left.bitraverseValidated({ it.invalid() }, { it.valid() }) shouldBe Invalid("foo")
    }

    "bisequenceValidated should be consistent with bitraverseValidated" {
      checkAll(Gen.either(Gen.string(), Gen.int())) { either ->
        either.bimap({ it.invalid() }, { it.valid() }).bisequenceValidated() ==
          either.bitraverseValidated({ it.invalid() }, { it.valid() })
      }
    }
  }
}

@Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
suspend fun handleWithPureFunction(a: Any, b: Any): Either<Throwable, Any> =
  b.right()

@Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
suspend fun handleWithPureFunction(throwable: Throwable): Either<Throwable, Unit> =
  Unit.right()

@Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
private suspend fun <A> throwException(
  a: A
): Either<Throwable, Any> =
  throw RuntimeException("An Exception is thrown while handling the result of the supplied function.")

private fun forAllSmallInt(fn: PropertyContext.(a: Int) -> Boolean) =
  checkAll(Gen.intSmall(), fn)

private fun forAllSmallInt(fn: PropertyContext.(a: Int, b: Int) -> Boolean) =
  checkAll(Gen.intSmall(), Gen.intSmall(), fn)

private fun forAllSmallInt(fn: PropertyContext.(a: Int, b: Int, c: Int) -> Boolean) =
  checkAll(Gen.intSmall(), Gen.intSmall(), Gen.intSmall(), fn)
