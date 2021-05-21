package arrow.core.test.generators

import arrow.core.Const
import arrow.core.Either
import arrow.core.Endo
import arrow.core.Eval
import arrow.core.Ior
import arrow.core.NonEmptyList
import arrow.core.NonEmptyList.Companion.fromListUnsafe
import arrow.core.Option
import arrow.core.Tuple10
import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.Tuple6
import arrow.core.Tuple7
import arrow.core.Tuple8
import arrow.core.Tuple9
import arrow.core.Validated
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.bool
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.file
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.localDateTime
import io.kotest.property.arbitrary.localTime
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.numericDoubles
import io.kotest.property.arbitrary.numericFloats
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.period
import io.kotest.property.arbitrary.short
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import kotlin.math.abs

fun <A, B> Arb.Companion.functionAToB(arb: Arb<B>): Arb<(A) -> B> =
  arb.map { b: B -> { _: A -> b } }

fun <A> Arb.Companion.functionAAToA(arb: Arb<A>): Arb<(A, A) -> A> =
  arb.map { a: A -> { _: A, _: A -> a } }

fun <A, B> Arb.Companion.functionBAToB(arb: Arb<B>): Arb<(B, A) -> B> =
  arb.map { b: B -> { _: B, _: A -> b } }

fun <A, B> Arb.Companion.functionABToB(arb: Arb<B>): Arb<(A, B) -> B> =
  arb.map { b: B -> { _: A, _: B -> b } }

fun <A> Arb.Companion.functionToA(arb: Arb<A>): Arb<() -> A> =
  arb.map { a: A -> { a } }

fun Arb.Companion.throwable(): Arb<Throwable> =
  Arb.of(listOf(RuntimeException(), NoSuchElementException(), IllegalArgumentException()))

fun Arb.Companion.fatalThrowable(): Arb<Throwable> =
  Arb.of(listOf(ThreadDeath(), StackOverflowError(), OutOfMemoryError(), InterruptedException()))

fun Arb.Companion.doubleSmall(): Arb<Double> =
  Arb.numericDoubles(from = 0.0, to = 100.0)

fun Arb.Companion.floatSmall(): Arb<Float> =
  Arb.numericFloats(from = 0F, to = 100F)

fun Arb.Companion.intSmall(factor: Int = 10000): Arb<Int> =
  Arb.int((Int.MIN_VALUE / factor)..(Int.MAX_VALUE / factor))

fun Arb.Companion.byteSmall(): Arb<Byte> =
  Arb.byte(min = (Byte.MIN_VALUE / 10).toByte(), max = (Byte.MAX_VALUE / 10).toByte())

fun Arb.Companion.shortSmall(): Arb<Short> {
  val range = (Short.MIN_VALUE / 1000)..(Short.MAX_VALUE / 1000)
  return Arb.short().filter { it in range }
}

fun Arb.Companion.longSmall(): Arb<Long> =
  Arb.long((Long.MIN_VALUE / 100000L)..(Long.MAX_VALUE / 100000L))

fun <A, B, C, D> Arb.Companion.tuple4(arbA: Arb<A>, arbB: Arb<B>, arbC: Arb<C>, arbD: Arb<D>): Arb<Tuple4<A, B, C, D>> =
  Arb.bind(arbA, arbB, arbC, arbD) { a: A, b: B, c: C, d: D -> Tuple4(a, b, c, d) }

fun <A, B, C, D, E> Arb.Companion.tuple5(
  arbA: Arb<A>,
  arbB: Arb<B>,
  arbC: Arb<C>,
  arbD: Arb<D>,
  arbE: Arb<E>
): Arb<Tuple5<A, B, C, D, E>> =
  Arb.bind(arbA, arbB, arbC, arbD, arbE) { a: A, b: B, c: C, d: D, e: E -> Tuple5(a, b, c, d, e) }

fun <A, B, C, D, E, F> Arb.Companion.tuple6(
  arbA: Arb<A>,
  arbB: Arb<B>,
  arbC: Arb<C>,
  arbD: Arb<D>,
  arbE: Arb<E>,
  arbF: Arb<F>
): Arb<Tuple6<A, B, C, D, E, F>> =
  Arb.bind(arbA, arbB, arbC, arbD, arbE, arbF) { a: A, b: B, c: C, d: D, e: E, f: F -> Tuple6(a, b, c, d, e, f) }

fun <A, B, C, D, E, F, G> Arb.Companion.tuple7(
  arbA: Arb<A>,
  arbB: Arb<B>,
  arbC: Arb<C>,
  arbD: Arb<D>,
  arbE: Arb<E>,
  arbF: Arb<F>,
  arbG: Arb<G>
): Arb<Tuple7<A, B, C, D, E, F, G>> =
  Arb.bind(arbA, arbB, arbC, arbD, arbE, arbF, arbG) { a: A, b: B, c: C, d: D, e: E, f: F, g: G ->
    Tuple7(
      a,
      b,
      c,
      d,
      e,
      f,
      g
    )
  }

fun <A, B, C, D, E, F, G, H> Arb.Companion.tuple8(
  arbA: Arb<A>,
  arbB: Arb<B>,
  arbC: Arb<C>,
  arbD: Arb<D>,
  arbE: Arb<E>,
  arbF: Arb<F>,
  arbG: Arb<G>,
  arbH: Arb<H>
): Arb<Tuple8<A, B, C, D, E, F, G, H>> =
  Arb.bind(
    Arb.tuple7(arbA, arbB, arbC, arbD, arbE, arbF, arbG),
    arbH
  ) { tuple: Tuple7<A, B, C, D, E, F, G>, h: H ->
    Tuple8(
      tuple.first,
      tuple.second,
      tuple.third,
      tuple.fourth,
      tuple.fifth,
      tuple.sixth,
      tuple.seventh,
      h
    )
  }

fun <A, B, C, D, E, F, G, H, I> Arb.Companion.tuple9(
  arbA: Arb<A>,
  arbB: Arb<B>,
  arbC: Arb<C>,
  arbD: Arb<D>,
  arbE: Arb<E>,
  arbF: Arb<F>,
  arbG: Arb<G>,
  arbH: Arb<H>,
  arbI: Arb<I>
): Arb<Tuple9<A, B, C, D, E, F, G, H, I>> =
  Arb.bind(
    Arb.tuple8(arbA, arbB, arbC, arbD, arbE, arbF, arbG, arbH),
    arbI
  ) { tuple: Tuple8<A, B, C, D, E, F, G, H>, i: I ->
    Tuple9(
      tuple.first,
      tuple.second,
      tuple.third,
      tuple.fourth,
      tuple.fifth,
      tuple.sixth,
      tuple.seventh,
      tuple.eighth,
      i
    )
  }

fun <A, B, C, D, E, F, G, H, I, J> Arb.Companion.tuple10(
  arbA: Arb<A>,
  arbB: Arb<B>,
  arbC: Arb<C>,
  arbD: Arb<D>,
  arbE: Arb<E>,
  arbF: Arb<F>,
  arbG: Arb<G>,
  arbH: Arb<H>,
  arbI: Arb<I>,
  arbJ: Arb<J>
): Arb<Tuple10<A, B, C, D, E, F, G, H, I, J>> =
  Arb.bind(
    Arb.tuple9(arbA, arbB, arbC, arbD, arbE, arbF, arbG, arbH, arbI),
    arbJ
  ) { tuple: Tuple9<A, B, C, D, E, F, G, H, I>, j: J ->
    Tuple10(
      tuple.first,
      tuple.second,
      tuple.third,
      tuple.fourth,
      tuple.fifth,
      tuple.sixth,
      tuple.seventh,
      tuple.eighth,
      tuple.ninth,
      j
    )
  }

fun Arb.Companion.nonZeroInt(): Arb<Int> = Arb.int().filter { it != 0 }

fun Arb.Companion.intPredicate(): Arb<(Int) -> Boolean> =
  Arb.nonZeroInt().flatMap { num ->
    val absNum = abs(num)
    Arb.of(
      listOf<(Int) -> Boolean>(
        { it > num },
        { it <= num },
        { it % absNum == 0 },
        { it % absNum == absNum - 1 }
      )
    )
  }

fun <A> Arb.Companion.endo(arb: Arb<A>): Arb<Endo<A>> = arb.map { a: A -> Endo<A> { a } }

fun <B> Arb.Companion.option(arb: Arb<B>): Arb<Option<B>> =
  arb.orNull().map { it.toOption() }

fun <E, A> Arb.Companion.either(arbE: Arb<E>, arbA: Arb<A>): Arb<Either<E, A>> {
  val arbLeft = arbE.map { Either.Left(it) }
  val arbRight = arbA.map { Either.Right(it) }
  return Arb.choice(arbLeft, arbRight)
}

fun <E, A> Arb<E>.or(arbA: Arb<A>): Arb<Either<E, A>> = Arb.either(this, arbA)

fun <E, A> Arb.Companion.validated(arbE: Arb<E>, arbA: Arb<A>): Arb<Validated<E, A>> =
  Arb.either(arbE, arbA).map { Validated.fromEither(it) }

fun <A> Arb.Companion.nonEmptyList(arb: Arb<A>): Arb<NonEmptyList<A>> =
  Arb.list(arb).filter(List<A>::isNotEmpty).map(::fromListUnsafe)

fun <A> Arb.Companion.sequence(arbA: Arb<A>): Arb<Sequence<A>> =
  Arb.list(arbA).map { it.asSequence() }

fun Arb.Companion.unit(): Arb<Unit> =
  Arb.constant(Unit)

fun <A, B> Arb.Companion.ior(arbA: Arb<A>, arbB: Arb<B>): Arb<Ior<A, B>> =
  arbA.alignWith(arbB) { it }

fun <A, B> Arb.Companion.arbConst(arb: Arb<A>): Arb<Const<A, B>> =
  arb.map { Const<A, B>(it) }

fun <A> Arb<A>.eval(): Arb<Eval<A>> =
  map { Eval.now(it) }

private fun <A, B, R> Arb<A>.alignWith(arbB: Arb<B>, transform: (Ior<A, B>) -> R): Arb<R> =
  Arb.bind(this, arbB) { a, b -> transform(Ior.Both(a, b)) }

fun Arb.Companion.suspendFunThatReturnsEitherAnyOrAnyOrThrows(): Arb<suspend () -> Either<Any, Any>> =
  choice(
    suspendFunThatReturnsAnyRight(),
    suspendFunThatReturnsAnyLeft(),
    suspendFunThatThrows()
  )

fun Arb.Companion.suspendFunThatReturnsAnyRight(): Arb<suspend () -> Either<Any, Any>> =
  any().map { suspend { it.right() } }

fun Arb.Companion.suspendFunThatReturnsAnyLeft(): Arb<suspend () -> Either<Any, Any>> =
  any().map { suspend { it.left() } }

fun Arb.Companion.suspendFunThatThrows(): Arb<suspend () -> Either<Any, Any>> =
  throwable().map { suspend { throw it } } as Arb<suspend () -> Either<Any, Any>>

fun Arb.Companion.suspendFunThatThrowsFatalThrowable(): Arb<suspend () -> Either<Any, Any>> =
  fatalThrowable().map { suspend { throw it } } as Arb<suspend () -> Either<Any, Any>>

fun Arb.Companion.any(): Arb<Any> =
  choice(
    Arb.string() as Arb<Any>,
    Arb.int() as Arb<Any>,
    Arb.long() as Arb<Any>,
    Arb.float() as Arb<Any>,
    Arb.double() as Arb<Any>,
    Arb.bool() as Arb<Any>,
    Arb.uuid() as Arb<Any>,
    Arb.file() as Arb<Any>,
    Arb.localDate() as Arb<Any>,
    Arb.localTime() as Arb<Any>,
    Arb.localDateTime() as Arb<Any>,
    Arb.period() as Arb<Any>,
    Arb.throwable() as Arb<Any>,
    Arb.fatalThrowable() as Arb<Any>,
    Arb.unit() as Arb<Any>
  )
