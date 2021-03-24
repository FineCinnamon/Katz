package arrow.optics.typeclasses

import arrow.core.Either
import arrow.core.Option
import arrow.core.Tuple2
import arrow.core.extensions.option.applicative.applicative
import arrow.core.fix
import arrow.core.identity
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import arrow.optics.Iso
import arrow.optics.Optional
import arrow.optics.PLens
import arrow.optics.Prism

typealias Conj<S, A> = Snoc<S, A>

/**
 * [Snoc] defines a [Prism] between a [S] and its [init] [S] and last element [A] and thus can be seen as the reverse of [Cons].
 * It provides a way to attach or detach elements on the end side of a structure.
 *
 * @param [S] source of [Prism] and init of [Prism] target.
 * @param [A] last of [Prism] focus, [A] is supposed to be unique for a given [S].
 */
fun interface Snoc<S, A> {

  /**
   * Provides a [Prism] between a [S] and its [init] [S] and last element [A].
   */
  fun snoc(): Prism<S, Tuple2<S, A>>

  /**
   * Provides an [Optional] between [S] and its init [S].
   */
  fun initOption(): Optional<S, S> = snoc() compose PLens.tuple2First()

  /**
   * Provides an [Optional] between [S] and its last element [A].
   */
  fun lastOption(): Optional<S, A> = snoc() compose PLens.tuple2Second()

  /**
   * Selects all elements except the last.
   */
  val S.init: Option<S>
    get() = initOption().getOption(this)

  /**
   * Append an element [A] to [S].
   */
  infix fun S.snoc(last: A): S =
    snoc().reverseGet(Tuple2(this, last))

  /**
   * Deconstruct an [S] between its [init] and last element.
   */
  fun S.unsnoc(): Option<Tuple2<S, A>> =
    snoc().getOption(this)

  companion object {

    /**
     * Lift an instance of [Snoc] using an [Iso].
     */
    fun <S, A, B> fromIso(SS: Snoc<A, B>, iso: Iso<S, A>): Snoc<S, B> =
      Snoc { iso compose SS.snoc() compose iso.reverse().first() }

    /**
     * Construct a [Snoc] instance from a [Prism].
     */
    operator fun <S, A> invoke(prism: Prism<S, Tuple2<S, A>>): Snoc<S, A> =
      Snoc { prism }

    /**
     * [Snoc] instance definition for [List].
     */
    @JvmStatic
    fun <A> list(): Snoc<List<A>, A> =
      Snoc {
        object : Prism<List<A>, Tuple2<List<A>, A>> {
          override fun getOrModify(s: List<A>): Either<List<A>, Tuple2<List<A>, A>> =
            Option.applicative().mapN(Option.just(s.dropLast(1)), s.lastOrNull().toOption(), ::identity)
              .fix()
              .toEither { s }

          override fun reverseGet(b: Tuple2<List<A>, A>): List<A> =
            b.a + b.b
        }
      }

    /**
     * [Snoc] instance for [String].
     */
    @JvmStatic
    fun string(): Snoc<String, Char> =
      Snoc {
        Prism(
          getOrModify = { if (it.isNotEmpty()) Tuple2(it.dropLast(1), it.last()).right() else it.left() },
          reverseGet = { (i, l) -> i + l }
        )
      }
  }
}
