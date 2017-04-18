package katz

typealias NonEmptyListKind<A> = HK<NonEmptyList.F, A>

/**
 * A List that can not be empty
 */
class NonEmptyList<out A> private constructor(
        val head: A,
        val tail: List<A>,
        val all: List<A>) : NonEmptyListKind<A> {

    class F private constructor()

    constructor(head: A, tail: List<A>) : this(head, tail, listOf(head) + tail)
    private constructor(list: List<A>) : this(list[0], list.drop(1), list)

    val size: Int = all.size

    fun contains(element: @UnsafeVariance A): Boolean {
        return (head == element).or(tail.contains(element))
    }

    fun containsAll(elements: Collection<@UnsafeVariance A>): Boolean =
            elements.all { contains(it) }

    fun isEmpty(): Boolean = false

    fun <B> map(f: (A) -> B): NonEmptyList<B> =
            NonEmptyList(f(head), tail.map(f))

    fun <B> flatMap(f: (A) -> NonEmptyList<B>): NonEmptyList<B> =
            f(head) + tail.flatMap { f(it).all }

    operator fun <A> NonEmptyList<A>.plus(l: NonEmptyList<A>): NonEmptyList<A> = NonEmptyList(all + l.all)

    operator fun <A> NonEmptyList<A>.plus(l: List<A>): NonEmptyList<A> = NonEmptyList(all + l)

    operator fun <A> NonEmptyList<A>.plus(a: A): NonEmptyList<A> = NonEmptyList(all + a)

    fun iterator(): Iterator<A> = all.iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as NonEmptyList<*>

        if (all != other.all) return false

        return true
    }

    override fun hashCode(): Int {
        return all.hashCode()
    }

    override fun toString(): String {
        return "NonEmptyList(all=$all)"
    }

    companion object : NonEmptyListMonad, GlobalInstance<Monad<NonEmptyList.F>>() {
        fun <A> of(head: A, vararg t: A): NonEmptyList<A> = NonEmptyList(head, t.asList())
    }
}
