package arrow.instances

import arrow.extension
import arrow.typeclasses.*

@extension
interface MonoidInvariantInstance<A> : Invariant<ForMonoid> {
    override fun <A, B> MonoidOf<A>.imap(f: (A) -> B, g: (B) -> A): Monoid<B> =
        object : Monoid<B> {
            override fun empty(): B = f(this@imap.fix().empty())

            override fun B.combine(b: B): B {
                val lhs = this
                return f(this@imap.fix().run { g(lhs).combine(g(b)) })
            }
        }
}