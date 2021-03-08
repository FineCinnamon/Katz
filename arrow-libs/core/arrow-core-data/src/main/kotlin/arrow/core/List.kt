@file:JvmMultifileClass

package arrow.core

import arrow.typeclasses.Monoid
import arrow.typeclasses.Semigroup
import kotlin.collections.plus as _plus

inline fun <B, C, D> mapN(
  b: Iterable<B>,
  c: Iterable<C>,
  map: (B, C) -> D
): List<D> =
  mapN(b, c, unit, unit, unit, unit, unit, unit, unit, unit) { b, c, _, _, _, _, _, _, _, _ -> map(b, c) }

inline fun <B, C, D, E> mapN(
  b: Iterable<B>,
  c: Iterable<C>,
  d: Iterable<D>,
  map: (B, C, D) -> E
): List<E> =
  mapN(b, c, d, unit, unit, unit, unit, unit, unit, unit) { b, c, d, _, _, _, _, _, _, _ -> map(b, c, d) }

inline fun <B, C, D, E, F> mapN(
  b: Iterable<B>,
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  map: (B, C, D, E) -> F
): List<F> =
  mapN(b, c, d, e, unit, unit, unit, unit, unit, unit) { b, c, d, e, _, _, _, _, _, _ -> map(b, c, d, e) }

inline fun <B, C, D, E, F, G> mapN(
  b: Iterable<B>,
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  map: (B, C, D, E, F) -> G
): List<G> =
  mapN(b, c, d, e, f, unit, unit, unit, unit, unit) { b, c, d, e, f, _, _, _, _, _ -> map(b, c, d, e, f) }

inline fun <B, C, D, E, F, G, H> mapN(
  b: Iterable<B>,
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  g: Iterable<G>,
  map: (B, C, D, E, F, G) -> H
): List<H> =
  mapN(b, c, d, e, f, g, unit, unit, unit, unit) { b, c, d, e, f, g, _, _, _, _ -> map(b, c, d, e, f, g) }

inline fun <B, C, D, E, F, G, H, I> mapN(
  b: Iterable<B>,
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  g: Iterable<G>,
  h: Iterable<H>,
  map: (B, C, D, E, F, G, H) -> I
): List<I> =
  mapN(b, c, d, e, f, g, h, unit, unit, unit) { b, c, d, e, f, g, h, _, _, _ -> map(b, c, d, e, f, g, h) }

inline fun <B, C, D, E, F, G, H, I, J> mapN(
  b: Iterable<B>,
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  g: Iterable<G>,
  h: Iterable<H>,
  i: Iterable<I>,
  map: (B, C, D, E, F, G, H, I) -> J
): List<J> =
  mapN(b, c, d, e, f, g, h, i, unit, unit) { b, c, d, e, f, g, h, i, _, _ -> map(b, c, d, e, f, g, h, i) }

inline fun <B, C, D, E, F, G, H, I, J, K> mapN(
  b: Iterable<B>,
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  g: Iterable<G>,
  h: Iterable<H>,
  i: Iterable<I>,
  j: Iterable<J>,
  map: (B, C, D, E, F, G, H, I, J) -> K
): List<K> =
  mapN(b, c, d, e, f, g, h, i, j, unit) { b, c, d, e, f, g, h, i, j, _ -> map(b, c, d, e, f, g, h, i, j) }

inline fun <B, C, D, E, F, G, H, I, J, K, L> mapN(
  b: Iterable<B>,
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  g: Iterable<G>,
  h: Iterable<H>,
  i: Iterable<I>,
  j: Iterable<J>,
  k: Iterable<K>,
  map: (B, C, D, E, F, G, H, I, J, K) -> L
): List<L> {
  val buffer = ArrayList<L>()
  for (bb in b) {
    for (cc in c) {
      for (dd in d) {
        for (ee in e) {
          for (ff in f) {
            for (gg in g) {
              for (hh in h) {
                for (ii in i) {
                  for (jj in j) {
                    for (kk in k) {
                      buffer.add(map(bb, cc, dd, ee, ff, gg, hh, ii, jj, kk))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  return buffer
}

/**
 * Returns a list containing all elements except the first element
 */
fun <T> List<T>.tail(): List<T> = this.drop(1)

infix fun <T> T.prependTo(list: List<T>): List<T> = listOf(this)._plus(list)

fun <T> List<Option<T>>.flatten(): List<T> = flatMap { it.fold(::emptyList, ::listOf) }

fun <A> Semigroup.Companion.list(): Semigroup<List<A>> =
  Monoid.list()

fun <A> Monoid.Companion.list(): Monoid<List<A>> =
  ListMonoid as Monoid<List<A>>

object ListMonoid : Monoid<List<Any?>> {
  override fun empty(): List<Any?> = emptyList()
  override fun List<Any?>.combine(b: List<Any?>): List<Any?> = this._plus(b)
}
