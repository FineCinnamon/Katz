package kategory

object State {
    operator fun <S, A> invoke(MF: Monad<IdHK> = Id.monad(), run: (S) -> Tuple2<S, A>): StateT<IdHK, S, A> = StateT(Id(run.andThen { Id(it) }))
}

fun <S, A> ((S) -> Tuple2<S, A>).state() : StateT<IdHK, S, A> = State(Id.monad(), this)

fun <S, A> StateT<IdHK, S, A>.run(initial: S): Tuple2<S, A> = this.ev().run(initial, Id.monad()).ev().value

fun <S, A> StateT<IdHK, S, A>.runA(s: S): A = run(s).b

fun <S, A> StateT<IdHK, S, A>.runS(s: S): S = run(s).a