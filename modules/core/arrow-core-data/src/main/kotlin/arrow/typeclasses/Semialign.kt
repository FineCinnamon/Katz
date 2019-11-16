package arrow.typeclasses

import arrow.Kind
import arrow.core.Ior
import arrow.core.identity

/**
 * A type class used for zipping and aligning of functors with non-uniform shapes.
 *
 * Note: Instances need to override either one of align/unlign here, otherwise a Stackoverflow exception will occur at runtime!
 */
interface Semialign<F> : Functor<F> {
  /**
   * Combines two structures by taking the union of their shapes and using Ior to hold the elements.
   */
  fun <A, B> align(left: Kind<F, A>, right: Kind<F, B>): Kind<F, Ior<A, B>> = alignWith(::identity, left, right)

  /**
   * Combines two structures by taking the union of their shapes and combining the elements with the given function.
   */
  fun <A, B, C> alignWith(fa: (Ior<A, B>) -> C, a: Kind<F, A>, b: Kind<F, B>): Kind<F, C> = align(a, b).map(fa)
}
