package katz

typealias IdKind<A> = HK<Id.F, A>

fun <A> IdKind<A>.ev(): Id<A> = this as Id<A>

fun <A> IdKind<A>.value(): A =
        this.ev().value

data class Id<out A>(val value: A) : IdKind<A> {

    class F private constructor()

    inline fun <B> map(f: (A) -> B): Id<B> =
            Id(f(value))

    inline fun <B> flatMap(f: (A) -> Id<B>): Id<B> =
            f(value)

    companion object : IdBimonad, GlobalInstance<Bimonad<Id.F>>()

}
