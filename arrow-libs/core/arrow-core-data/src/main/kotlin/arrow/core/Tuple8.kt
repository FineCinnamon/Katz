@file:JvmMultifileClass
@file:JvmName("TupleNKt")

package arrow.core

data class Tuple8<out A, out B, out C, out D, out E, out F, out G, out H>(
  @Deprecated("Use first instead", ReplaceWith("first"))
  val a: A,
  @Deprecated("Use second instead", ReplaceWith("second"))
  val b: B,
  @Deprecated("Use third instead", ReplaceWith("third"))
  val c: C,
  @Deprecated("Use fourth instead", ReplaceWith("fourth"))
  val d: D,
  @Deprecated("Use fifth instead", ReplaceWith("fifth"))
  val e: E,
  @Deprecated("Use sixth instead", ReplaceWith("sixth"))
  val f: F,
  @Deprecated("Use seventh instead", ReplaceWith("seventh"))
  val g: G,
  @Deprecated("Use eighth instead", ReplaceWith("eighth"))
  val h: H
) {

  val first: A = a
  val second: B = b
  val third: C = c
  val fourth: D = d
  val fifth: E = e
  val sixth: F = f
  val seventh: G = g
  val eighth: H = h

  override fun toString(): String =
    "($a, $b, $c, $d, $e, $f, $g, $h)"

  companion object
}

operator fun <A : Comparable<A>, B : Comparable<B>, C : Comparable<C>, D : Comparable<D>, E : Comparable<E>, F : Comparable<F>, G : Comparable<G>, H : Comparable<H>>
Tuple8<A, B, C, D, E, F, G, H>.compareTo(other: Tuple8<A, B, C, D, E, F, G, H>): Int {
  val first = a.compareTo(other.a)
  return if (first == 0) {
    val second = b.compareTo(other.b)
    if (second == 0) {
      val third = c.compareTo(other.c)
      if (third == 0) {
        val fourth = d.compareTo(other.d)
        if (fourth == 0) {
          val fifth = e.compareTo(other.e)
          if (fifth == 0) {
            val sixth = f.compareTo(other.f)
            if (sixth == 0) {
              val seventh = g.compareTo(other.g)
              if (seventh == 0) h.compareTo(other.h)
              else seventh
            } else sixth
          } else fifth
        } else fourth
      } else third
    } else second
  } else first
}
