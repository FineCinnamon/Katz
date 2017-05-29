package katz

fun <A> HK<Eval.F, A>.ev(): Eval<A> = this as Eval<A>

/**
 * Eval is a monad which controls evaluation of a value or a computation that produces a value.
 *
 * Three basic evaluation strategies:
 *
 *  - Now:    evaluated immediately
 *  - Later:  evaluated once when value is needed
 *  - Always: evaluated every time value is needed
 *
 * The Later and Always are both lazy strategies while Now is eager.
 * Later and Always are distinguished from each other only by
 * memoization: once evaluated Later will save the value to be returned
 * immediately if it is needed again. Always will run its computation
 * every time.
 *
 * Eval supports stack-safe lazy computation via the .map and .flatMap
 * methods, which use an internal trampoline to avoid stack overflows.
 * Computation done within .map and .flatMap is always done lazily,
 * even when applied to a Now instance.
 *
 * It is not generally good style to pattern-match on Eval instances.
 * Rather, use .map and .flatMap to chain computation, and use .value
 * to get the result when needed. It is also not good style to create
 * Eval instances whose computation involves calling .value on another
 * Eval instance -- this can defeat the trampolining and lead to stack
 * overflows.
 */
sealed class Eval<out A> : HK<Eval.F, A> {
    class F private constructor()

    abstract fun value(): A

    abstract fun memoize(): Eval<A>

    fun <B> map(f: (A) -> B): Eval<B> =
            flatMap { a -> Now(f(a)) }

    fun <B> flatMap(f: (A) -> Eval<B>): Eval<B> =
            when (this) {
                is Eval.Compute<A> -> object : Compute<B>() {
                    override fun <S> start(): Eval<S> = (this@Eval).start()
                    override fun <S> run(s: S): Eval<B> =
                            object : Compute<B>() {
                                override fun <S1> start(): Eval<S1> = (this@Eval).run(s) as Eval<S1>
                                override fun <S1> run(s: S1): Eval<B> = f(s as A)
                            }
                }
                is Eval.Call<A> -> object : Eval.Compute<B>() {
                    override fun <S> start(): Eval<S> = this@Eval.thunk() as Eval<S>
                    override fun <S> run(s: S): Eval<B> = f(s as A)
                }
                else -> object : Eval.Compute<B>() {
                    override fun <S> start(): Eval<S> = this@Eval as Eval<S>
                    override fun <S> run(s: S): Eval<B> = f(s as A)
                }
            }

    /**
     * Construct an eager Eval<A> instance. In some sense it is equivalent to using a val.
     *
     * This type should be used when an A value is already in hand, or when the computation to produce an A value is
     * pure and very fast.
     */
    data class Now<out A>(val value: A) : Eval<A>() {
        override fun value(): A = value
        override fun memoize(): Eval<A> = this
    }

    /**
     * Construct a lazy Eval<A> instance.
     *
     * This type should be used for most "lazy" values. In some sense it is equivalent to using a lazy val.
     *
     * When caching is not required or desired (e.g. if the value produced may be large) prefer Always. When there
     * is no computation necessary, prefer Now.
     *
     * Once Later has been evaluated, the closure (and any values captured by the closure) will not be retained, and
     * will be available for garbage collection.
     */
    data class Later<out A>(private val f: () -> A) : Eval<A>() {
        val value: A by lazy(f)

        override fun value(): A = value
        override fun memoize(): Eval<A> = this
    }

    /**
     * Construct a lazy Eval<A> instance.
     *
     * This type can be used for "lazy" values. In some sense it is equivalent to using a Function0 value.
     *
     * This type will evaluate the computation every time the value is required. It should be avoided except when
     * laziness is required and caching must be avoided. Generally, prefer Later.
     */
    data class Always<out A>(private val f: () -> A) : Eval<A>() {
        override fun value(): A = f()
        override fun memoize(): Eval<A> = Later(f)
    }

    /**
     * Call is a type of Eval<A> that is used to defer computations which produce Eval<A>.
     *
     * Users should not instantiate Call instances themselves. Instead, they will be automatically created when needed.
     */
    data class Call<out A>(val thunk: () -> Eval<A>) : Eval<A>() {
        override fun memoize(): Eval<A> = Later { value() }
        override fun value(): A = collapse(this).value()

        companion object {
            /**
             * Collapse the call stack for eager evaluations.
             */
            fun <A> collapse(fa: Eval<A>): Eval<A> {
                var lfa = fa
                loop@ while (true) {
                    when (lfa) {
                        is Call -> {
                            lfa = lfa.thunk()
                        }
                        is Compute -> {
                            val clfa: Compute<A> = lfa
                            object : Compute<A>() {
                                override fun <S> start(): Eval<S> = clfa.start()
                                override fun <S> run(s: S): Eval<A> {
                                    lfa = clfa.run(s)
                                    return lfa
                                }
                            }
                        }
                        else -> break@loop
                    }
                }
                return lfa
            }
        }
    }

    /**
     * Compute is a type of Eval<A> that is used to chain computations involving .map and .flatMap. Along with
     * Eval#flatMap. It implements the trampoline that guarantees stack-safety.
     *
     * Users should not instantiate Compute instances themselves. Instead, they will be automatically created when
     * needed.
     *
     * Unlike a traditional trampoline, the internal workings of the trampoline are not exposed. This allows a slightly
     * more efficient implementation of the .value method.
     */
    internal abstract class Compute<out A> : Eval<A>() {

        abstract fun <S> start(): Eval<S>

        abstract fun <S> run(s: S): Eval<A>

        override fun memoize(): Eval<A> =
                Later { value() }

        override fun value(): A {
            var curr: Eval<A> = this
            var fs: List<(Any?) -> Eval<A>> = listOf()

            loop@ while (true) {
                when (curr) {
                    is Compute -> {
                        val currComp: Compute<A> = curr
                        currComp.start<A>().let { cc ->
                            when (cc) {
                                is Compute -> {
                                    val inStartFun: (Any?) -> Eval<A> = { cc.run(it) }
                                    val outStartFun: (Any?) -> Eval<A> = { currComp.run(it) }
                                    curr = cc.start<A>()
                                    fs = listOf(inStartFun, outStartFun) + fs
                                }
                                else -> {
                                    curr = currComp.run(cc.value())
                                }
                            }
                        }
                    }
                    else ->
                        if (fs.isNotEmpty()) {
                            curr = fs[0](curr.value())
                            fs = fs.drop(1)
                        } else {
                            break@loop
                        }
                }
            }
            return curr.value()
        }
    }

    companion object : EvalMonad, GlobalInstance<Monad<Eval.F>>() {
        @JvmStatic fun <A> now(a: A) = Now(a)
        @JvmStatic fun <A> later(f: () -> A) = Later(f)
        @JvmStatic fun <A> always(f: () -> A) = Always(f)
        @JvmStatic fun <A> defer(f: () -> Eval<A>): Eval<A> = Call(f)
        @JvmStatic fun raise(t: Throwable): Eval<Nothing> = defer { throw t }

        @JvmStatic val Unit: Eval<Unit> = Now(kotlin.Unit)
        @JvmStatic val True: Eval<Boolean> = Now(true)
        @JvmStatic val False: Eval<Boolean> = Now(false)
        @JvmStatic val Zero: Eval<Int> = Now(0)
        @JvmStatic val One: Eval<Int> = Now(1)
    }
}
