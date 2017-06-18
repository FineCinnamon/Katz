package kategory

typealias StateTKind<F, S, A> = HK3<StateT.F, F, S, A>
typealias StateTF<F, S> = HK2<StateT.F, F, S>

typealias StateTFun<F, S, A> = (S) -> HK<F, Tuple2<S, A>>
typealias StateTFunKind<F, S, A> = HK<F, StateTFun<F, S, A>>

fun <F, S, A> StateTKind<F, S, A>.ev(): StateT<F, S, A> =
        this as StateT<F, S, A>

class StateT<F, S, A>(
        val MF: Monad<F>,
        val runF: StateTFunKind<F, S, A>
) : StateTKind<F, S, A> {
    class F private constructor()

    companion object {
        inline operator fun <reified F, S, A> invoke(run: StateTFunKind<F, S, A>, MF: Monad<F> = monad<F>()): StateT<F, S, A> =
                StateT(MF, run)
    }

    fun <B> map(f: (A) -> B): StateT<F, S, B> =
            transform { (s, a) -> Tuple2(s, f(a)) }

    fun <B, Z> map2(sb: StateT<F, S, B>, fn: (A, B) -> Z): StateT<F, S, Z> =
            applyF(MF.map2(runF, sb.runF) { (ssa, ssb) ->
                ssa.andThen { fsa ->
                    MF.flatMap(fsa) { (s, a) ->
                        MF.map(ssb(s)) { (s, b) -> Tuple2(s, fn(a, b)) }
                    }
                }
            }, MF)

    fun <B, Z> map2Eval(sb: Eval<StateT<F, S, B>>, fn: (A, B) -> Z): Eval<StateT<F, S, Z>> =
            MF.map2Eval(runF, sb.map { it.runF }) { (ssa, ssb) ->
                ssa.andThen { fsa ->
                    MF.flatMap(fsa) { (s, a) ->
                        MF.map(ssb((s))) { (s, b) -> Tuple2(s, fn(a, b)) }
                    }
                }
            }.map { applyF(it, MF) }

    fun <B> product(sb: StateT<F, S, B>): StateT<F, S, Tuple2<A, B>> =
            map2(sb) { a, b -> Tuple2(a, b) }

    fun <B> flatMap(fas: (A) -> StateTKind<F, S, B>): StateT<F, S, B> =
            applyF(
                    MF.map(runF) { sfsa ->
                        sfsa.andThen { fsa ->
                            MF.flatMap(fsa) {
                                fas(it.b).ev().run(it.a)
                            }
                        }
                    }
                    , MF)

    fun <B> flatMapF(faf: (A) -> HK<F, B>): StateT<F, S, B> =
            applyF(
                    MF.map(runF) { sfsa ->
                        sfsa.andThen { fsa ->
                            MF.flatMap(fsa) { (s, a) ->
                                MF.map(faf(a)) { b -> Tuple2(s, b) }
                            }
                        }
                    }
                    , MF)

    fun <B> transform(f: (Tuple2<S, A>) -> Tuple2<S, B>): StateT<F, S, B> =
            applyF(
                    MF.map(runF) { sfsa ->
                        sfsa.andThen { fsa ->
                            MF.map(fsa, f)
                        }
                    }, MF)

    fun <F, S, A> applyF(runF: StateTFunKind<F, S, A>, MF: Monad<F>): StateT<F, S, A> =
            StateT(MF, runF)

    fun run(initial: S): HK<F, Tuple2<S, A>> =
            MF.flatMap(runF) { f -> f(initial) }

    fun runA(s: S): HK<F, A> =
            MF.map(run(s)) { it.b }

    fun runS(s: S): HK<F, S> =
            MF.map(run(s)) { it.a }
}

