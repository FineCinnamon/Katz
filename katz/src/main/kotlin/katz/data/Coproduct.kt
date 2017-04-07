/*
 * Copyright (C) 2017 The Katz Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package katz

typealias CoproductF<F> = HK<Coproduct.F, F>
typealias CoproductFG<F, G> = HK<CoproductF<F>, G>
typealias CoproductKind<F, G, A> = HK<CoproductFG<F, G>, A>

fun <F, G, A> CoproductKind<F, G, A>.ev(): Coproduct<F, G, A> = this as Coproduct<F, G, A>

data class Coproduct<F, G, A>(val CF: Comonad<F>, val CG: Comonad<G>, val run: Either<HK<F, A>, HK<G, A>>) : CoproductKind<F, G, A> {

    class F private constructor()

    fun <B> map(f: (A) -> B): Coproduct<F, G, B> {
        return Coproduct(CF, CG, run.bimap(CF.lift(f), CG.lift(f)))
    }

    fun <B> coflatMap(f: (Coproduct<F, G, A>) -> B): Coproduct<F, G, B> =
            Coproduct(CF, CG, run.bimap(
                    { CF.coflatMap(it, { f(Coproduct(CF, CG, Either.Left(it))) }) },
                    { CG.coflatMap(it, { f(Coproduct(CF, CG, Either.Right(it))) }) }
            ))

    fun extract(): A =
            run.fold({ CF.extract(it) }, { CG.extract(it) })

    fun <H> fold(f: FunctionK<F, H>, g: FunctionK<G, H>): HK<H, A> =
            run.fold({ f(it) }, { g(it) })

    companion object {
        inline fun <reified F, reified G, A> invoke(CF: Comonad<F> = comonad<F>(), CG: Comonad<G> = comonad<G>(), run: Either<HK<F, A>, HK<G, A>>) =
                Coproduct(CF, CG, run)
    }

}

