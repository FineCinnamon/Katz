package katz

typealias NonEmptyListKind<A> = HK<NonEmptyList.F, A>

fun <A> NonEmptyListKind<A>.ev(): NonEmptyList<A> =
        this as NonEmptyList<A>

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

    fun contains(element: @UnsafeVariance A): Boolean =
            (head == element).or(tail.contains(element))

    fun containsAll(elements: Collection<@UnsafeVariance A>): Boolean =
            elements.all { contains(it) }

    fun isEmpty(): Boolean = false

    fun <B> map(f: (A) -> B): NonEmptyList<B> =
            NonEmptyList(f(head), tail.map(f))

    fun <B> flatMap(f: (A) -> NonEmptyList<B>): NonEmptyList<B> =
            f(head) + tail.flatMap { f(it).all }

    operator fun plus(l: NonEmptyList<@UnsafeVariance A>): NonEmptyList<A> =
            NonEmptyList(all + l.all)

    operator fun plus(l: List<@UnsafeVariance A>): NonEmptyList<A> =
            NonEmptyList(all + l)

    operator fun plus(a: @UnsafeVariance A): NonEmptyList<A> =
            NonEmptyList(all + a)

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

    companion object : NonEmptyListBimonad, GlobalInstance<Bimonad<NonEmptyList.F>>() {
        @JvmStatic fun <A> of(head: A, vararg t: A): NonEmptyList<A> = NonEmptyList(head, t.asList())
        @JvmStatic fun <A> fromList(l: List<A>): Option<NonEmptyList<A>> = if (l.isEmpty()) Option.None else Option.Some(NonEmptyList(l))
        @JvmStatic fun <A> fromListUnsafe(l: List<A>): NonEmptyList<A> = NonEmptyList(l)
    }
}
