package arrow.core

import arrow.higherkind

fun <I, O> ((I) -> O).k(): Function1<I, O> = Function1(this)

operator fun <I, O> Function1Of<I, O>.invoke(i: I): O = this.fix().f(i)

@higherkind
class Function1<I, out O>(val f: (I) -> O) : Function1Of<I, O> {

  fun <B> map(f: (O) -> B): Function1<I, B> = f.compose { a: I -> this.f(a) }.k()

  fun <B> flatMap(f: (O) -> Function1Of<I, B>): Function1<I, B> = { p: I -> f(this.f(p))(p) }.k()

  fun <B> apPipe(ff: Function1Of<I, (O) -> B>): Function1<I, B> =
    ff.fix().flatMap { f -> map(f) }.fix()

  fun local(f: (I) -> I): Function1<I, O> = f.andThen { this(it) }.k()

  fun <B> compose(g: Function1<B, I>): Function1<B, O> = f.compose(g.f).k()

  companion object {

    fun <I> ask(): Function1<I, I> = { a: I -> a }.k()

    fun <I, A> just(a: A): Function1<I, A> = { _: I -> a }.k()

    tailrec private fun <I, A, B> step(a: A, t: I, fn: (A) -> Function1Of<I, Either<A, B>>): B {
      val af = fn(a)(t)
      return when (af) {
        is Either.Right -> af.b
        is Either.Left -> step(af.a, t, fn)
      }
    }

    fun <I, A, B> tailRecM(a: A, f: (A) -> Function1Of<I, Either<A, B>>): Function1<I, B> = { t: I -> step(a, t, f) }.k()

    fun <I> id(): Function1<I, I> = Function1(::identity)
  }
}

@Suppress("NOTHING_TO_INLINE")
inline infix fun <I, O, B> Function1Of<I, (O) -> B>.ap(fo: Function1Of<I, O>): Function1<I, B> =
  fo.fix().apPipe(this)
