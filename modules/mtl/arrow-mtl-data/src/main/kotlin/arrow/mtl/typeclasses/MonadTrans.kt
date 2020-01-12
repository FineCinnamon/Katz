package arrow.mtl.typeclasses

import arrow.Kind
import arrow.Kind2
import arrow.typeclasses.Monad

/**
 * The `MonadTrans` typeclass abstracts the ability to create a monad transformer.
 */
interface MonadTrans<T> {
  /**
   * transform a given monad `Kind<F, A>` to `Kind2<T, F, A>`
   *
   * {: data-executable='true'}
   *
   * ```kotlin:ank
   * import arrow.mtl.extensions.optiont.monadTrans.monadTrans
   * import arrow.core.extensions.*
   * import arrow.core.*
   * import arrow.mtl.*
   *
   * fun main(args: Array<String>) {
   *    // sampleStart
   *    val result = OptionT.monadTrans().run {
   *      lift(Id.monad(Id.just("hello"))
   *    }
   *    // sampleEnd
   *    println(result)
   * }
   * ```
   */
  fun <F, A> lift(MF: Monad<F>, fa: Kind<F, A>): Kind2<T, F, A>
}
